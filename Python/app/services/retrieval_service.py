from typing import Protocol

from app.schemas.retrieval import SimilarCase
from app.services.embedding_service import TextEmbedder

# Umbral propuesto en el plan (sección 9); se calibrará con datos reales.
DEFAULT_MIN_SCORE = 0.75
MAX_SIMILAR_CASES = 3


class SimilarityStore(Protocol):
    """Busca en el histórico los casos más cercanos a un vector."""

    def find_similar(
        self,
        embedding: list[float],
        exclude_incident_id: int,
        limit: int,
        min_score: float,
    ) -> list[SimilarCase]: ...


def find_similar_cases(
    embedder: TextEmbedder,
    store: SimilarityStore,
    incident_id: int,
    title: str,
    description: str,
    top_k: int = MAX_SIMILAR_CASES,
    min_score: float = DEFAULT_MIN_SCORE,
) -> list[SimilarCase]:
    """Devuelve hasta 3 incidentes resueltos e indexados parecidos al actual (RF-11, RF-13).

    El propio incidente, los no resueltos, los no indexados y los que no superan
    el umbral quedan fuera de la consulta. Un fallo de base de datos sube como
    AiUnavailableError (lo lanza el repositorio).
    """
    embedding = embedder.embed(f"{title}\n{description}")#esto lo convierte en embeddings 
    limit = max(0, min(top_k, MAX_SIMILAR_CASES))
    if limit == 0:
        return []
    cases = store.find_similar(embedding, incident_id, limit, min_score)
    # Defensa en el servicio: el tope de 3 y el orden no dependen del almacén.
    return sorted(cases, key=lambda case: case.score, reverse=True)[:limit]
