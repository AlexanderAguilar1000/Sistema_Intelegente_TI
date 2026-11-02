"""Servicio de clasificación de incidentes (RF-4).

Recibe el título y la descripción de un incidente, le pide a Groq que lo clasifique y
comprueba que la respuesta use solo valores del catálogo recibido del backend.

Flujo de `classify`:
    1. Construye el prompt (instrucciones + texto del incidente + catálogo permitido).
    2. Llama a Groq a través del cliente (`client.complete`).
    3. Convierte la respuesta de texto a JSON y extrae tipo, prioridad y área.
    4. Valida que cada valor esté en el catálogo; si no, lanza un error (HTTP 422).

Este módulo no sabe nada de HTTP ni de la librería de Groq: recibe el cliente ya
construido. Por eso en los tests se le pasa un Groq simulado.
"""

import json
from dataclasses import dataclass
from time import perf_counter

from app.clients.groq_client import LlmClient
from app.core.errors import InvalidClassificationError
from app.schemas.classification import Catalog
from app.services.prompting import DO_NOT_OBEY_INSTRUCTION, wrap_untrusted

# Instrucción fija para el modelo. Le pide devolver solo JSON con tres claves y
# limitarse a los valores de las listas que se le dan en el mensaje del usuario.
SYSTEM_PROMPT = (
    "Eres un clasificador de incidentes de TI. Responde únicamente con un objeto JSON "
    'con las claves "type", "priority" y "area", usando solo valores de las listas dadas. '
    + DO_NOT_OBEY_INSTRUCTION
)


@dataclass(frozen=True)
class Classification:
    """Resultado de clasificar un incidente.

    `frozen=True` lo hace inmutable: una vez creado no se puede modificar.
    Los nombres de campo coinciden con los de la respuesta HTTP.
    """

    type: str  # tipo de incidente, p. ej. "BUG"
    priority: str  # prioridad, p. ej. "HIGH"
    area: str  # área responsable, p. ej. "APPLICATIONS"
    model: str  # modelo de Groq que respondió
    elapsed_ms: int  # milisegundos que tardó la llamada a Groq


def build_user_prompt(title: str, description: str, catalog: Catalog) -> str:
    """Arma el mensaje del usuario: el incidente y las listas de valores permitidos.

    Las listas salen del `catalog` recibido en la petición, no están fijas en el
    código: el backend es la única fuente de verdad de los catálogos (decisión D4).
    """
    return (
        f"{wrap_untrusted(title, description)}\n\n"
        f"Tipos permitidos: {', '.join(catalog.types)}\n"
        f"Prioridades permitidas: {', '.join(catalog.priorities)}\n"
        f"Áreas permitidas: {', '.join(catalog.areas)}"
    )


def classify(
    client: LlmClient, title: str, description: str, catalog: Catalog
) -> Classification:
    """Pide la clasificación a Groq y valida que cada valor esté en el catálogo recibido.

    Args:
        client: cliente del LLM (Groq real en producción, uno simulado en los tests).
        title: título del incidente.
        description: descripción del incidente.
        catalog: valores permitidos de tipo, prioridad y área.

    Returns:
        La clasificación con los tres valores validados, el modelo usado y el tiempo
        que tardó Groq.

    Raises:
        AiUnavailableError: la lanza `client.complete` si Groq falla o no está
            configurado (la API lo convierte en 503).
        InvalidClassificationError: si la respuesta no es un JSON con las tres
            claves, o algún valor no está en el catálogo (la API lo convierte en 422).
    """
    # Se mide solo la llamada a Groq, que es la parte lenta.
    started = perf_counter()
    reply = client.complete(
        SYSTEM_PROMPT,
        build_user_prompt(
            title, description, catalog
        ),  # le doy la información  y le doy el prompt
    )
    elapsed_ms = int((perf_counter() - started) * 1000)

    # Groq devuelve texto: puede no ser JSON, no ser un objeto o faltar alguna clave.
    # ValueError: JSON mal formado. KeyError: falta una clave.
    # TypeError: el JSON no es un objeto (p. ej. una lista o un número).
    try:
        data = json.loads(reply)  # el texto que devuelve groq lo convierto a json
        values = (
            data["type"],
            data["priority"],
            data["area"],
        )  # extraigo los valores de tipo, prioridad y area del json
    except (ValueError, KeyError, TypeError) as error:
        raise InvalidClassificationError("respuesta de Groq no válida") from error

    # Aunque el prompt lo pide, el modelo puede inventarse un valor. Por eso se
    # comprueba cada uno contra el catálogo antes de darlo por bueno.
    type_, priority, area = (
        values  # aca verifica que los valores que devuelva groq  esten dentro de los valores de type , priority , area que estan en el sistema
    )
    if (
        type_ not in catalog.types
        or priority not in catalog.priorities
        or area not in catalog.areas
    ):
        raise InvalidClassificationError("valor fuera de catálogo")
    return Classification(type_, priority, area, client.model_name, elapsed_ms)
