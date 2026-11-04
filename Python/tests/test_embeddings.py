"""Pruebas de T-26: indexar un incidente resuelto en el histórico (RF-13)."""

from fastapi.testclient import TestClient

from app.clients.embedder import EMBEDDING_DIMENSIONS, MODEL_NAME
from app.core.errors import AiUnavailableError
from app.main import create_app

REQUEST = {
    "incident_id": 42,
    "title": "La aplicación de nóminas falla al guardar",
    "description": "Al pulsar guardar aparece un error 500 desde esta mañana.",
    "solution": "Se reinició el pool de conexiones y se corrigió el parámetro de timeout.",
    "type": "BUG",
    "area": "APPLICATIONS",
}


class FakeEmbedder:
    """Embedder determinista: no carga el modelo real."""

    model_name = MODEL_NAME
    dimensions = EMBEDDING_DIMENSIONS

    def __init__(self) -> None:
        self.texts: list[str] = []

    def embed(self, text: str) -> list[float]:
        self.texts.append(text)
        return [float(len(text) % 7)] * EMBEDDING_DIMENSIONS


class FakeRepository:
    """Repositorio en memoria con la misma semántica de upsert que la tabla real."""

    def __init__(self, error: Exception | None = None) -> None:
        self.rows: dict[int, tuple[list[float], str]] = {}
        self.writes = 0
        self._error = error

    def upsert(self, incident_id: int, embedding: list[float], model_name: str) -> None:
        if self._error is not None:
            raise self._error
        self.writes += 1
        self.rows[incident_id] = (embedding, model_name)


def post(
    embedder: FakeEmbedder,
    repository: FakeRepository,
    body: dict | None = None,
):
    app = create_app(embedder_loader=lambda: embedder, embedding_repository=repository)
    with TestClient(app) as client:
        return client.post("/ai/v1/embeddings", json=body or REQUEST)


def test_indexa_y_responde_con_modelo_y_dimensiones() -> None:
    response = post(FakeEmbedder(), FakeRepository())

    assert response.status_code == 200
    assert response.json() == {
        "indexed": True,
        "model": "paraphrase-multilingual-MiniLM-L12-v2",
        "dimensions": 384,
    }


def test_guarda_un_vector_de_384_dimensiones_para_el_incidente() -> None:
    repository = FakeRepository()

    post(FakeEmbedder(), repository)

    embedding, model_name = repository.rows[42]
    assert len(embedding) == 384
    assert model_name == "paraphrase-multilingual-MiniLM-L12-v2"


def test_el_texto_vectorizado_une_titulo_descripcion_y_solucion() -> None:
    embedder = FakeEmbedder()

    post(embedder, FakeRepository())

    assert len(embedder.texts) == 1
    for part in ("title", "description", "solution"):
        assert REQUEST[part] in embedder.texts[0]


def test_llamarlo_dos_veces_deja_una_sola_fila_actualizada() -> None:
    repository = FakeRepository()
    embedder = FakeEmbedder()
    changed = {**REQUEST, "solution": "Solución corregida tras una segunda revisión."}

    post(embedder, repository)
    post(embedder, repository, changed)

    assert repository.writes == 2
    assert list(repository.rows) == [42]  # una sola fila
    assert len(embedder.texts) == 2


def test_fallo_de_la_base_de_datos_devuelve_503() -> None:
    repository = FakeRepository(error=AiUnavailableError("Base de datos no disponible"))

    response = post(FakeEmbedder(), repository)

    assert response.status_code == 503
    assert response.json() == {
        "error_code": "AI_UNAVAILABLE",
        "detail": "Base de datos no disponible",
    }


def test_error_inesperado_al_guardar_se_traduce_a_503() -> None:
    repository = FakeRepository(error=RuntimeError("conexión rechazada"))

    response = post(FakeEmbedder(), repository)

    assert response.status_code == 503
    assert response.json()["error_code"] == "AI_UNAVAILABLE"


def test_sin_solucion_la_peticion_es_invalida() -> None:
    body = {key: value for key, value in REQUEST.items() if key != "solution"}

    response = post(FakeEmbedder(), FakeRepository(), body)

    assert response.status_code == 422
