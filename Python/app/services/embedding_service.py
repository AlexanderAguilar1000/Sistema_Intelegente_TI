from typing import Protocol

from app.core.errors import AiUnavailableError
from app.schemas.embedding import EmbeddingRequest


class TextEmbedder(Protocol):
    """Genera el vector de un texto."""

    model_name: str
    dimensions: int

    def embed(self, text: str) -> list[float]: ...


class EmbeddingStore(Protocol):
    """Guarda el vector de un incidente (una sola fila por incidente)."""

    def upsert(
        self, incident_id: int, embedding: list[float], model_name: str
    ) -> None: ...


def build_text(request: EmbeddingRequest) -> str:
    """Texto que se vectoriza: título, descripción y solución (RF-13)."""
    return f"{request.title}\n{request.description}\n{request.solution}"


def index_incident(
    embedder: TextEmbedder, store: EmbeddingStore, request: EmbeddingRequest
) -> None:
    """Vectoriza el incidente resuelto y lo deja en el histórico (RF-13).

    Cualquier fallo al guardar se traduce a AiUnavailableError para que el
    backend marque el incidente como no indexado sin perder la solución (RNF-8).
    """
    embedding = embedder.embed(build_text(request))
    try:
        store.upsert(request.incident_id, embedding, embedder.model_name)
    except AiUnavailableError:
        raise
    except Exception as error:
        raise AiUnavailableError(
            "No se pudo guardar el embedding en la base de datos"
        ) from error
