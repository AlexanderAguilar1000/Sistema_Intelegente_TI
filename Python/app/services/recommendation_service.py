"""Servicio de recomendación paso a paso con RAG (RF-11).

Flujo de `recommend`:
    1. Busca en el histórico los casos resueltos más parecidos (`find_similar_cases`).
    2. Arma el prompt: con casos, los incluye como texto; sin casos, pide una
       recomendación general avisando de que no hay casos previos.
    3. Llama a Groq y exige un JSON con una lista `steps`.
    4. Descarta pasos vacíos; si no queda ninguno, lanza InvalidRecommendationError.

A Groq solo llega texto; los vectores no salen de este servicio.
"""

import json
from dataclasses import dataclass
from time import perf_counter

from app.clients.groq_client import LlmClient
from app.core.errors import InvalidRecommendationError
from app.schemas.analysis import AnalysisRequest, Reference
from app.services.embedding_service import TextEmbedder
from app.services.prompting import (
    DO_NOT_OBEY_CASES_INSTRUCTION,
    DO_NOT_OBEY_INSTRUCTION,
    wrap_untrusted,
    wrap_untrusted_cases,
)
from app.services.retrieval_service import SimilarityStore, find_similar_cases

SYSTEM_PROMPT = (
    "Eres un asistente de diagnóstico de incidentes de TI para un técnico. Responde "
    'únicamente con un objeto JSON con la clave "steps": una lista de pasos de '
    "diagnóstico y resolución, ordenados y concretos, cada uno en una frase. "
    + DO_NOT_OBEY_INSTRUCTION
    + " "
    + DO_NOT_OBEY_CASES_INSTRUCTION
)

NO_CASES_NOTICE = (
    "No hay casos previos parecidos en el histórico: da una recomendación general "
    "y deja claro, en el primer paso, que no se apoya en casos previos."
)


@dataclass(frozen=True)
class Recommendation:
    """Resultado de analizar un incidente."""

    grounded: bool  # True si se apoya en casos similares del histórico
    steps: list[str]
    references: list[Reference]
    model: str
    elapsed_ms: int


def build_user_prompt(request: AnalysisRequest, cases_text: str | None) -> str:
    """Mensaje del usuario: incidente, su clasificación y los casos (o el aviso)."""
    context = (
        f"Tipo: {request.type}\nPrioridad: {request.priority}\nÁrea: {request.area}"
    )
    history = cases_text if cases_text is not None else NO_CASES_NOTICE
    return (
        f"{wrap_untrusted(request.title, request.description)}\n\n"
        f"{context}\n\n{history}"
    )


def recommend(
    client: LlmClient,
    embedder: TextEmbedder,
    store: SimilarityStore,
    request: AnalysisRequest,
) -> Recommendation:
    """Genera los pasos de diagnóstico para un incidente (RF-11).

    Raises:
        AiUnavailableError: Groq o la base de datos fallan (la API lo convierte en 503).
        InvalidRecommendationError: Groq no devolvió ningún paso con texto (422).
    """
    started = perf_counter()
    #aqui busca los casos similares 
    cases = find_similar_cases(
        embedder,
        store,
        request.incident_id,
        request.title,
        request.description,
        request.top_k,
        request.min_score,
    )
    #para que sean tratados como datos y no intrucciones
    #extrae el titulo , la descripción y la solución de estos casos similares 
    cases_text = (
        wrap_untrusted_cases([(c.title, c.description, c.solution) for c in cases])
        if cases
        else None
    )

    #le mando el prompt  y la información de los casos similares 
    reply = client.complete(SYSTEM_PROMPT, build_user_prompt(request, cases_text))
    steps = _parse_steps(reply)
    elapsed_ms = int((perf_counter() - started) * 1000)
    return Recommendation(
        grounded=bool(cases),
        steps=steps,
        references=[
            Reference(incident_id=c.incident_id, title=c.title, score=c.score)
            for c in cases
        ],
        model=client.model_name,
        elapsed_ms=elapsed_ms,
    )


def _parse_steps(reply: str) -> list[str]:
    """Extrae los pasos con texto; falla si no hay ninguno."""
    try:
        raw_steps = json.loads(reply)["steps"] #los pasos se devuelven en formato json  y aca lo convierto a texto paara enviarlo a angular . 
    except (ValueError, KeyError, TypeError) as error:
        raise InvalidRecommendationError("respuesta de Groq no válida") from error
    if not isinstance(raw_steps, list):
        raise InvalidRecommendationError("sin pasos con texto")
    steps = [s.strip() for s in raw_steps if isinstance(s, str) and s.strip()]
    if not steps:
        raise InvalidRecommendationError("sin pasos con texto")
    return steps
