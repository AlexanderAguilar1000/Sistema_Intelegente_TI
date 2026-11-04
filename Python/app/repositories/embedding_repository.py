import psycopg
from pgvector.psycopg import register_vector

from app.core.errors import AiUnavailableError
from app.schemas.retrieval import SimilarCase

# Única pieza que escribe y consulta incident_embeddings (plan, sección 1.3 y D2).
# No crea ni altera el esquema: lo gestiona Flyway desde Spring Boot (principio 5).
_UPSERT_SQL = """
    INSERT INTO incident_embeddings (incident_id, embedding, model_name, indexed_at)
    VALUES (%s, %s, %s, now())
    ON CONFLICT (incident_id) DO UPDATE
    SET embedding = EXCLUDED.embedding,
        model_name = EXCLUDED.model_name,
        indexed_at = now()
"""


# Similitud coseno = 1 - distancia coseno (<=>). Solo entran incidentes resueltos e
# indexados (RF-13); el inner join descarta los que no tienen vector.
_SIMILAR_SQL = """
    SELECT i.id, i.title, i.description, i.solution,
           1 - (e.embedding <=> %(query)s::vector) AS score
    FROM incident_embeddings e
    JOIN incidents i ON i.id = e.incident_id
    WHERE i.status = 'RESOLVED'
      AND i.indexed = TRUE
      AND i.id <> %(exclude_id)s
      AND 1 - (e.embedding <=> %(query)s::vector) >= %(min_score)s
    ORDER BY e.embedding <=> %(query)s::vector
    LIMIT %(limit)s
"""


class EmbeddingRepository:
    """Acceso a la tabla incident_embeddings en PostgreSQL+pgvector."""

    def __init__(self, database_url: str, connect_timeout: int = 5) -> None:
        self._database_url = database_url
        self._connect_timeout = connect_timeout

#guarda en la base de datos el vector númerico del incidente
    def upsert(self, incident_id: int, embedding: list[float], model_name: str) -> None:
        """Inserta el vector del incidente o reescribe el existente (idempotente)."""
        try:
            with psycopg.connect(
                self._database_url, connect_timeout=self._connect_timeout
            ) as conn:
                register_vector(conn)
                conn.execute(_UPSERT_SQL, (incident_id, embedding, model_name))
        except psycopg.Error as error:
            raise AiUnavailableError(
                "No se pudo guardar el embedding en la base de datos"
            ) from error

    def find_similar(
        self,
        embedding: list[float],
        exclude_incident_id: int,
        limit: int,
        min_score: float,
    ) -> list[SimilarCase]:
        """Los casos resueltos e indexados más cercanos que superan el umbral (RF-11)."""
        params = {
            "query": embedding,
            "exclude_id": exclude_incident_id,
            "min_score": min_score,
            "limit": limit,
        }
        try:
            with psycopg.connect(
                self._database_url, connect_timeout=self._connect_timeout
            ) as conn:
                register_vector(conn)
                rows = conn.execute(_SIMILAR_SQL, params).fetchall()
        except psycopg.Error as error:
            raise AiUnavailableError(
                "No se pudo consultar el histórico en la base de datos"
            ) from error
        return [
            SimilarCase(
                incident_id=row[0],
                title=row[1],
                description=row[2],
                solution=row[3],
                score=float(row[4]),
            )
            for row in rows
        ]
