"""Pruebas de T-23: arranque del servicio de IA con el modelo cargado (RF-4, RF-13)."""

from fastapi.testclient import TestClient

from app.infrastructure.embedder import EMBEDDING_DIMENSIONS, MODEL_NAME, Embedder
from app.main import create_app


class FakeEmbedder:
    """Sustituto ligero del embedder real, para no cargar el modelo en cada test."""

    model_name = MODEL_NAME
    dimensions = EMBEDDING_DIMENSIONS


def test_health_responde_con_el_modelo_en_uso() -> None:
    with TestClient(create_app(embedder_loader=FakeEmbedder)) as client:
        response = client.get("/ai/v1/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "model": "paraphrase-multilingual-MiniLM-L12-v2",
        "dimensions": 384,
    }


def test_el_modelo_se_carga_una_sola_vez_al_inicio() -> None:
    loads: list[int] = []

    def loader() -> FakeEmbedder:
        loads.append(1)
        return FakeEmbedder()

    with TestClient(create_app(embedder_loader=loader)) as client:
        assert len(loads) == 1  # cargado al arrancar, antes de recibir peticiones
        for _ in range(3):
            client.get("/ai/v1/health")

    assert len(loads) == 1  # las peticiones reutilizan el mismo modelo


def test_el_embedder_real_carga_paraphrase_multilingual_de_384_dimensiones() -> None:
    embedder = Embedder.load()

    assert embedder.model_name == "paraphrase-multilingual-MiniLM-L12-v2"
    assert embedder.dimensions == 384
