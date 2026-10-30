from fastapi import APIRouter, Request
from pydantic import BaseModel

router = APIRouter(prefix="/ai/v1")


class HealthResponse(BaseModel):
    status: str
    model: str
    dimensions: int

#cuando llamen a esta ruta como ya va a estar cargado el modelo lo va a devolver 
@router.get("/health")
def health(request: Request) -> HealthResponse:
    """Comprobación de arranque: indica el modelo de embeddings cargado."""
    embedder = request.app.state.embedder
    #Esto es lo que devuelve 
    return HealthResponse(
        status="ok", model=embedder.model_name, dimensions=embedder.dimensions
    )
