"""Pruebas de T-27: encontrar los incidentes más parecidos (RF-11, RF-13).

El servicio se prueba con un embedder y un almacén simulados y deterministas; el
filtrado real (umbral, propio incidente, no resueltos, no indexados) se comprueba
contra PostgreSQL en test_similarity_repository.py.
"""

from app.clients.embedder import EMBEDDING_DIMENSIONS, MODEL_NAME
from app.schemas.retrieval import SimilarCase
from app.services.retrieval_service import (
    DEFAULT_MIN_SCORE,
    MAX_SIMILAR_CASES,
    find_similar_cases,
)


class FakeEmbedder:
    model_name = MODEL_NAME
    dimensions = EMBEDDING_DIMENSIONS

    def __init__(self) -> None:
        self.texts: list[str] = []

    def embed(self, text: str) -> list[float]:
        self.texts.append(text)
        return [1.0] * EMBEDDING_DIMENSIONS


class FakeStore:
    """Almacén que devuelve casos fijos y recuerda con qué parámetros se le consultó."""

    def __init__(self, cases: list[SimilarCase] | None = None) -> None:
        self.cases = cases or []
        self.calls: list[dict[str, object]] = []

    def find_similar(
        self,
        embedding: list[float],
        exclude_incident_id: int,
        limit: int,
        min_score: float,
    ) -> list[SimilarCase]:
        self.calls.append(
            {
                "embedding": embedding,
                "exclude_incident_id": exclude_incident_id,
                "limit": limit,
                "min_score": min_score,
            }
        )
        return self.cases


def make_case(incident_id: int, score: float) -> SimilarCase:
    return SimilarCase(
        incident_id=incident_id,
        title=f"Incidente {incident_id}",
        description="descripción",
        solution="Se reinició el servicio y se corrigió la configuración.",
        score=score,
    )


def test_vectoriza_titulo_y_descripcion_del_incidente_actual() -> None:
    embedder = FakeEmbedder()
    store = FakeStore()

    find_similar_cases(embedder, store, 42, "Falla nóminas", "Error 500 al guardar")

    assert embedder.texts == ["Falla nóminas\nError 500 al guardar"]
    assert store.calls[0]["embedding"] == [1.0] * EMBEDDING_DIMENSIONS


def test_excluye_el_propio_incidente_en_la_consulta() -> None:
    store = FakeStore()

    find_similar_cases(FakeEmbedder(), store, 42, "t", "d")

    assert store.calls[0]["exclude_incident_id"] == 42


def test_usa_el_umbral_y_el_tope_por_defecto() -> None:
    store = FakeStore()

    find_similar_cases(FakeEmbedder(), store, 1, "t", "d")

    assert store.calls[0]["min_score"] == DEFAULT_MIN_SCORE == 0.75
    assert store.calls[0]["limit"] == MAX_SIMILAR_CASES == 3


def test_nunca_pide_mas_de_tres_casos() -> None:
    store = FakeStore()

    find_similar_cases(FakeEmbedder(), store, 1, "t", "d", top_k=10)

    assert store.calls[0]["limit"] == 3


def test_respeta_un_top_k_menor_y_un_umbral_propio() -> None:
    store = FakeStore()

    find_similar_cases(FakeEmbedder(), store, 1, "t", "d", top_k=2, min_score=0.9)

    assert store.calls[0]["limit"] == 2
    assert store.calls[0]["min_score"] == 0.9


def test_devuelve_como_mucho_tres_casos_ordenados_por_parecido() -> None:
    cases = [
        make_case(i, score) for i, score in [(1, 0.8), (2, 0.95), (3, 0.85), (4, 0.9)]
    ]
    store = FakeStore(cases)

    result = find_similar_cases(FakeEmbedder(), store, 99, "t", "d")

    assert [case.incident_id for case in result] == [2, 4, 3]


def test_sin_casos_devuelve_lista_vacia() -> None:
    assert find_similar_cases(FakeEmbedder(), FakeStore(), 1, "t", "d") == []
