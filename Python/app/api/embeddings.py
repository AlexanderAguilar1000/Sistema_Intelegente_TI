from fastapi import APIRouter, Request

from app.schemas.embedding import EmbeddingRequest, EmbeddingResponse
from app.services.embedding_service import index_incident

router = APIRouter(prefix="/ai/v1")


@router.post("/embeddings")
def embeddings(body: EmbeddingRequest, request: Request) -> EmbeddingResponse:
    """Indexa un incidente resuelto en incident_embeddings (RF-13)."""
    embedder = request.app.state.embedder#accede al modelo cargado 
    index_incident(embedder, request.app.state.embedding_repository, body)
    return EmbeddingResponse(
        indexed=True, model=embedder.model_name, dimensions=embedder.dimensions
    )
