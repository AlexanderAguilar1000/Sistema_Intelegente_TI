from pydantic import BaseModel


class EmbeddingRequest(BaseModel):
    incident_id: int
    title: str
    description: str
    solution: str
    type: str
    area: str


class EmbeddingResponse(BaseModel):
    indexed: bool
    model: str
    dimensions: int
