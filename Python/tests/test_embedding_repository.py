"""Pruebas de T-26 contra PostgreSQL+pgvector real (plan, sección 6, punto 4).

Usan una base de test aparte (por ejemplo ti_incidentes_test) con las migraciones de
Flyway ya aplicadas. Se activan con la variable de entorno TEST_DATABASE_URL, p. ej.
postgresql://postgres:***@localhost:5432/ti_incidentes_test; sin ella se omiten.
"""

import os
from collections.abc import Iterator

import psycopg
import pytest

from app.repositories.embedding_repository import EmbeddingRepository

TEST_DATABASE_URL = os.environ.get("TEST_DATABASE_URL")

pytestmark = pytest.mark.skipif(
    not TEST_DATABASE_URL, reason="TEST_DATABASE_URL no definida"
)


@pytest.fixture
def incident_id() -> Iterator[int]:
    """Crea un incidente resuelto mínimo (por la FK) y lo borra al terminar."""
    assert TEST_DATABASE_URL is not None
    with psycopg.connect(TEST_DATABASE_URL, autocommit=True) as conn:
        user_id = conn.execute(
            "INSERT INTO users (username, full_name, role, area) "
            "VALUES ('t26_tech', 'Técnico T26', 'TECHNICIAN', 'APPLICATIONS') "
            "RETURNING id"
        ).fetchone()[0]
        new_id = conn.execute(
            "INSERT INTO incidents (title, description, status, classification_status, "
            "type, priority, area, created_by, assigned_to, solution, indexed, closed_at) "
            "VALUES ('t', 'd', 'RESOLVED', 'AI_CLASSIFIED', 'BUG', 'HIGH', 'APPLICATIONS', "
            "%s, %s, %s, FALSE, now()) RETURNING id",
            (user_id, user_id, "s" * 40),
        ).fetchone()[0]
    yield new_id
    with psycopg.connect(TEST_DATABASE_URL, autocommit=True) as conn:
        conn.execute("DELETE FROM incidents WHERE id = %s", (new_id,))
        conn.execute("DELETE FROM users WHERE id = %s", (user_id,))


def rows_for(incident_id: int) -> list[tuple[int, str]]:
    assert TEST_DATABASE_URL is not None
    with psycopg.connect(TEST_DATABASE_URL) as conn:
        return conn.execute(
            "SELECT vector_dims(embedding), model_name "
            "FROM incident_embeddings WHERE incident_id = %s",
            (incident_id,),
        ).fetchall()


def test_upsert_deja_una_fila_de_384_dimensiones(incident_id: int) -> None:
    assert TEST_DATABASE_URL is not None
    repository = EmbeddingRepository(TEST_DATABASE_URL)

    repository.upsert(incident_id, [0.1] * 384, "modelo-a")

    assert rows_for(incident_id) == [(384, "modelo-a")]


def test_upsert_dos_veces_actualiza_la_misma_fila(incident_id: int) -> None:
    assert TEST_DATABASE_URL is not None
    repository = EmbeddingRepository(TEST_DATABASE_URL)

    repository.upsert(incident_id, [0.1] * 384, "modelo-a")
    repository.upsert(incident_id, [0.2] * 384, "modelo-b")

    assert rows_for(incident_id) == [(384, "modelo-b")]


def test_base_inaccesible_lanza_ai_unavailable() -> None:
    from app.core.errors import AiUnavailableError

    repository = EmbeddingRepository(
        "postgresql://nadie:x@127.0.0.1:1/inexistente", connect_timeout=1
    )

    with pytest.raises(AiUnavailableError):
        repository.upsert(1, [0.0] * 384, "modelo")
