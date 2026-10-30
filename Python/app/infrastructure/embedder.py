from typing import Protocol

from sentence_transformers import SentenceTransformer

# Cambiar de modelo obliga a una migración y a reindexar todo el histórico (plan, sección 9).
MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2"
EMBEDDING_DIMENSIONS = 384

#Este embeder lo que hace es cargar el modelo 
class EmbedderInfo(Protocol):
    """Datos del modelo de embeddings en uso."""

    model_name: str
    dimensions: int


class Embedder:
    """Envuelve el modelo de sentence-transformers, que se carga una sola vez."""

    def __init__(self, model: SentenceTransformer) -> None:
        self._model = model
        self.model_name = MODEL_NAME
        self.dimensions = model.get_embedding_dimension() or 0
        if self.dimensions != EMBEDDING_DIMENSIONS:
            raise RuntimeError(
                f"El modelo {MODEL_NAME} devuelve {self.dimensions} dimensiones; "
                f"se esperaban {EMBEDDING_DIMENSIONS}"
            )

    @classmethod
    def load(cls) -> "Embedder":
        """Carga el modelo desde la caché local (o lo descarga la primera vez)."""
        return cls(SentenceTransformer(MODEL_NAME))#carga el modelo  que desgarde de hugging face 
