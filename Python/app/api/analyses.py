from fastapi import APIRouter, Request

from app.schemas.analysis import AnalysisRequest, AnalysisResponse
from app.services.recommendation_service import recommend

router = APIRouter(prefix="/ai/v1")


@router.post("/analyses")
def analyses(body: AnalysisRequest, request: Request) -> AnalysisResponse:
    """Recomienda pasos de diagnóstico apoyándose en incidentes similares (RF-11)."""
    result = recommend(
        request.app.state.groq,
        request.app.state.embedder,
        request.app.state.embedding_repository,
        body,
    )
    return AnalysisResponse(
        grounded=result.grounded,
        steps=result.steps,
        references=result.references,
        model=result.model,
        elapsed_ms=result.elapsed_ms,
    )
