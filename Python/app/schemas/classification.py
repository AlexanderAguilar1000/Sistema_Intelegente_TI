from pydantic import BaseModel


class Catalog(BaseModel):
    types: list[str]
    priorities: list[str]
    areas: list[str]


class ClassificationRequest(BaseModel):
    incident_id: int
    title: str
    description: str
    catalog: Catalog


class ClassificationResponse(BaseModel):
    type: str
    priority: str
    area: str
    model: str
    elapsed_ms: int
