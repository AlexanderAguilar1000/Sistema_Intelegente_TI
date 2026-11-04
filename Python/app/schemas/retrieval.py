from pydantic import BaseModel


class SimilarCase(BaseModel):
    """Incidente resuelto del histórico, recuperado por parecido (RF-11).

    Solo lleva texto y la puntuación: el vector nunca sale del repositorio.
    """

    incident_id: int
    title: str
    description: str
    solution: str
    score: float
