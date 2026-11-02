from fastapi import APIRouter, Request

from app.schemas.classification import ClassificationRequest, ClassificationResponse
from app.services.classification_service import classify

router = APIRouter(prefix="/ai/v1")


@router.post("/classifications")
def classifications(
    body: ClassificationRequest, request: Request
) -> ClassificationResponse:
    """Clasifica un incidente con Groq contra el catálogo recibido (RF-4)."""
    result = classify(
        request.app.state.groq, body.title, body.description, body.catalog
    )
    return ClassificationResponse(
        type=result.type,
        priority=result.priority,
        area=result.area,
        model=result.model,
        elapsed_ms=result.elapsed_ms,
    )
