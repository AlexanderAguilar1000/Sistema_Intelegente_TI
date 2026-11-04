import psycopg
from pgvector.psycopg import register_vector

from app.core.errors import AiUnavailableError

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


class EmbeddingRepository:
    """Acceso a la tabla incident_embeddings en PostgreSQL+pgvector."""

    def __init__(self, database_url: str, connect_timeout: int = 5) -> None:
        self._database_url = database_url
        self._connect_timeout = connect_timeout

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
