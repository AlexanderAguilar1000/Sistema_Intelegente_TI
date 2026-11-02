"""Pruebas de T-24: clasificar un incidente con Groq simulado (RF-4)."""

import json

from fastapi.testclient import TestClient

from app.clients.embedder import EMBEDDING_DIMENSIONS, MODEL_NAME
from app.core.errors import AiUnavailableError
from app.main import create_app

CATALOG = {
    "types": ["BUG", "PERFORMANCE", "ACCESS", "HARDWARE", "NETWORK", "CONFIGURATION"],
    "priorities": ["HIGH", "MEDIUM", "LOW"],
    "areas": ["INFRASTRUCTURE", "APPLICATIONS", "DATABASE", "SECURITY", "USER_SUPPORT"],
}

REQUEST = {
    "incident_id": 42,
    "title": "La aplicación de nóminas falla al guardar",
    "description": "Al pulsar guardar aparece un error 500 desde esta mañana.",
    "catalog": CATALOG,
}


class FakeEmbedder:
    model_name = MODEL_NAME
    dimensions = EMBEDDING_DIMENSIONS


class FakeGroq:
    """Groq simulado: devuelve el texto indicado o lanza el error indicado."""

    model_name = "groq-simulado"

    def __init__(
        self, reply: str | None = None, error: Exception | None = None
    ) -> None:
        self._reply = reply
        self._error = error
        self.calls: list[tuple[str, str]] = []

    def complete(self, system: str, user: str) -> str:
        self.calls.append((system, user))
        if self._error is not None:
            raise self._error
        assert self._reply is not None
        return self._reply


def post(groq: FakeGroq, body: dict | None = None):
    app = create_app(embedder_loader=FakeEmbedder, groq_client=groq)
    with TestClient(app) as client:
        return client.post("/ai/v1/classifications", json=body or REQUEST)


def test_clasifica_con_valores_del_catalogo() -> None:
    reply = json.dumps({"type": "BUG", "priority": "HIGH", "area": "APPLICATIONS"})
    groq = FakeGroq(reply=reply)

    response = post(groq)

    assert response.status_code == 200
    body = response.json()
    assert body["type"] == "BUG"
    assert body["priority"] == "HIGH"
    assert body["area"] == "APPLICATIONS"
    assert body["model"] == "groq-simulado"
    assert isinstance(body["elapsed_ms"], int) and body["elapsed_ms"] >= 0
    assert len(groq.calls) == 1


def test_el_prompt_incluye_el_incidente_y_el_catalogo_recibido() -> None:
    reply = json.dumps({"type": "BUG", "priority": "HIGH", "area": "APPLICATIONS"})
    groq = FakeGroq(reply=reply)

    post(groq)

    prompt = " ".join(groq.calls[0])
    assert REQUEST["title"] in prompt
    assert REQUEST["description"] in prompt
    assert "PERFORMANCE" in prompt and "USER_SUPPORT" in prompt


def test_valor_fuera_de_catalogo_devuelve_422() -> None:
    reply = json.dumps({"type": "VIRUS", "priority": "HIGH", "area": "APPLICATIONS"})

    response = post(FakeGroq(reply=reply))

    assert response.status_code == 422
    assert response.json() == {
        "error_code": "INVALID_CLASSIFICATION",
        "detail": "valor fuera de catálogo",
    }


def test_respuesta_de_groq_que_no_es_json_devuelve_422() -> None:
    response = post(FakeGroq(reply="no sé qué decirte"))

    assert response.status_code == 422
    assert response.json()["error_code"] == "INVALID_CLASSIFICATION"


def test_respuesta_de_groq_sin_un_campo_devuelve_422() -> None:
    response = post(FakeGroq(reply=json.dumps({"type": "BUG", "priority": "HIGH"})))

    assert response.status_code == 422
    assert response.json()["error_code"] == "INVALID_CLASSIFICATION"


def test_groq_caido_devuelve_503() -> None:
    response = post(FakeGroq(error=AiUnavailableError("Groq no responde")))

    assert response.status_code == 503
    assert response.json() == {
        "error_code": "AI_UNAVAILABLE",
        "detail": "Groq no responde",
    }
