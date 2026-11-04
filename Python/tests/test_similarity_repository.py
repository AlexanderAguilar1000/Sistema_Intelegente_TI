"""Pruebas de T-27 contra PostgreSQL+pgvector real (plan, sección 6, punto 4).

Siembran incidentes con embeddings deterministas (vectores unitarios con un coseno
conocido respecto a la consulta). Usan la base de test con las migraciones de Flyway
aplicadas y se activan con TEST_DATABASE_URL; sin ella se omiten.
"""

import math
import os
from collections.abc import Iterator

import psycopg
import pytest
from pgvector.psycopg import register_vector

from app.clients.embedder import EMBEDDING_DIMENSIONS
from app.repositories.embedding_repository import EmbeddingRepository

TEST_DATABASE_URL = os.environ.get("TEST_DATABASE_URL")

pytestmark = pytest.mark.skipif(
    not TEST_DATABASE_URL, reason="TEST_DATABASE_URL no definida"
)

SOLUTION = "Se reinició el servicio y se corrigió la configuración."


def vector_with_cosine(cosine: float) -> list[float]:
    """Vector unitario cuyo coseno con la consulta (eje 0) es exactamente `cosine`."""
    vector = [0.0] * EMBEDDING_DIMENSIONS
    vector[0] = cosine
    vector[1] = math.sqrt(1.0 - cosine * cosine)
    return vector


QUERY = vector_with_cosine(1.0)


class Seed:
    """Siembra incidentes y los borra al terminar."""

    def __init__(self, conn: psycopg.Connection) -> None:
        self._conn = conn
        self._user_id = conn.execute(
            "INSERT INTO users (username, full_name, role, area) "
            "VALUES ('t27_tech', 'Técnico T27', 'TECHNICIAN', 'APPLICATIONS') "
            "RETURNING id"
        ).fetchone()[0]
        self.incident_ids: list[int] = []

    def add(
        self,
        title: str,
        cosine: float | None,
        *,
        status: str = "RESOLVED",
        indexed: bool = True,
    ) -> int:
        """Crea un incidente; con `cosine` None queda sin fila en incident_embeddings."""
        if status == "RESOLVED":
            sql = (
                "INSERT INTO incidents (title, description, status, classification_status, "
                "type, priority, area, created_by, assigned_to, solution, indexed, closed_at) "
                "VALUES (%s, 'descripción', 'RESOLVED', 'AI_CLASSIFIED', 'BUG', 'HIGH', "
                "'APPLICATIONS', %s, %s, %s, %s, now()) RETURNING id"
            )
            params: tuple[object, ...] = (
                title,
                self._user_id,
                self._user_id,
                SOLUTION,
                indexed,
            )
        else:  # IN_PROGRESS: sin solución ni cierre
            sql = (
                "INSERT INTO incidents (title, description, status, classification_status, "
                "type, priority, area, created_by, assigned_to, indexed) "
                "VALUES (%s, 'descripción', 'IN_PROGRESS', 'AI_CLASSIFIED', 'BUG', 'HIGH', "
                "'APPLICATIONS', %s, %s, %s) RETURNING id"
            )
            params = (title, self._user_id, self._user_id, indexed)
        incident_id = self._conn.execute(sql, params).fetchone()[0]
        self.incident_ids.append(incident_id)
        if cosine is not None:
            self._conn.execute(
                "INSERT INTO incident_embeddings (incident_id, embedding, model_name) "
                "VALUES (%s, %s, 'modelo-test')",
                (incident_id, vector_with_cosine(cosine)),
            )
        return incident_id

    def cleanup(self) -> None:
        for incident_id in self.incident_ids:
            self._conn.execute("DELETE FROM incidents WHERE id = %s", (incident_id,))
        self._conn.execute("DELETE FROM users WHERE id = %s", (self._user_id,))


@pytest.fixture
def seed() -> Iterator[Seed]:
    assert TEST_DATABASE_URL is not None
    with psycopg.connect(TEST_DATABASE_URL, autocommit=True) as conn:
        register_vector(conn)
        seeded = Seed(conn)
        yield seeded
        seeded.cleanup()


@pytest.fixture
def repository() -> EmbeddingRepository:
    assert TEST_DATABASE_URL is not None
    return EmbeddingRepository(TEST_DATABASE_URL)


def search(
    repository: EmbeddingRepository,
    seed: Seed,
    exclude: int = 0,
    limit: int = 3,
    min_score: float = 0.75,
) -> list:
    """Busca y deja solo los casos sembrados por esta prueba (la base puede tener más)."""
    cases = repository.find_similar(QUERY, exclude, limit, min_score)
    return [case for case in cases if case.incident_id in seed.incident_ids]


def test_devuelve_como_mucho_tres_casos_ordenados_por_parecido(
    repository: EmbeddingRepository, seed: Seed
) -> None:
    ids = {c: seed.add(f"caso {c}", c) for c in (0.80, 0.99, 0.90, 0.85, 0.95)}

    cases = search(repository, seed)

    assert [case.incident_id for case in cases] == [ids[0.99], ids[0.95], ids[0.90]]
    assert cases[0].score == pytest.approx(0.99, abs=1e-4)
    assert cases[0].solution == SOLUTION


def test_descarta_los_que_no_superan_el_umbral(
    repository: EmbeddingRepository, seed: Seed
) -> None:
    parecido = seed.add("parecido", 0.90)
    seed.add("lejano", 0.50)

    cases = search(repository, seed, min_score=0.75)

    assert [case.incident_id for case in cases] == [parecido]


def test_excluye_el_propio_incidente(
    repository: EmbeddingRepository, seed: Seed
) -> None:
    propio = seed.add("propio", 1.0)
    otro = seed.add("otro", 0.90)

    cases = search(repository, seed, exclude=propio)

    assert [case.incident_id for case in cases] == [otro]


def test_excluye_los_no_resueltos(repository: EmbeddingRepository, seed: Seed) -> None:
    resuelto = seed.add("resuelto", 0.90)
    seed.add("en progreso", 0.99, status="IN_PROGRESS", indexed=False)

    cases = search(repository, seed)

    assert [case.incident_id for case in cases] == [resuelto]


def test_excluye_los_no_indexados(repository: EmbeddingRepository, seed: Seed) -> None:
    indexado = seed.add("indexado", 0.90)
    seed.add("sin fila de vector", None)
    seed.add("marcado sin indexar", 0.99, indexed=False)

    cases = search(repository, seed)

    assert [case.incident_id for case in cases] == [indexado]


def test_base_inaccesible_lanza_ai_unavailable() -> None:
    from app.core.errors import AiUnavailableError

    repository = EmbeddingRepository(
        "postgresql://nadie:x@127.0.0.1:1/inexistente", connect_timeout=1
    )

    with pytest.raises(AiUnavailableError):
        repository.find_similar(QUERY, 1, 3, 0.75)
