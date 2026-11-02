class AiUnavailableError(Exception):
    """Groq no responde o falla (se traduce a 503 AI_UNAVAILABLE)."""


class InvalidClassificationError(Exception):
    """Groq respondió algo que no cumple el catálogo (se traduce a 422)."""
