from pydantic import BaseModel


class AnalysisRequest(BaseModel):
    incident_id: int
    title: str
    description: str
    type: str
    priority: str
    area: str
    top_k: int = 3
    min_score: float = 0.75


class Reference(BaseModel):
    incident_id: int
    title: str
    score: float


class AnalysisResponse(BaseModel):
    grounded: bool
    steps: list[str]
    references: list[Reference]
    model: str
    elapsed_ms: int
