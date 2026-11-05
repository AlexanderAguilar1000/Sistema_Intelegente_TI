import os
from typing import Protocol

from groq import Groq, GroqError

from app.core.errors import AiUnavailableError

# Plazo propio para no colgar al backend, que espera como máximo 10 s (RNF-1).
TIMEOUT_SECONDS = 8.0


class LlmClient(Protocol):
    """Lo que el dominio necesita del proveedor del LLM."""

    model_name: str

    def complete(self, system: str, user: str) -> str:
        """Devuelve el JSON de la respuesta del modelo; lanza AiUnavailableError si falla."""
        ...


class GroqClient:
    """Cliente de Groq; la clave y el modelo vienen de variables de entorno (RNF-3)."""

    def __init__(self) -> None:
        self.model_name = os.getenv("GROQ_MODEL", "")
        self._client: Groq | None = None

#le digo que se cargue el modelo de groq le paso lo que envia el usuario yel prompt que utiliza el sistema que es clasificar 
# le doy roles  y solo lo conecto 
    def complete(self, system: str, user: str) -> str:
        try:
            response = self._groq().chat.completions.create(#Aca se conecta con groq
                model=self.model_name,
                messages=[
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
                response_format={"type": "json_object"},
                temperature=0,
            )
        except GroqError as error: #si hay un error en el groq que no permite clasificar el incidente entonces lanza un error 
            raise AiUnavailableError("Groq no está disponible") from error
        return response.choices[0].message.content or ""

    def _groq(self) -> Groq:#Aqui se conecta con groq para poder mas adelante clasificar un incidente .
        api_key = os.getenv("GROQ_API_KEY")
        if not api_key or not self.model_name:
            raise AiUnavailableError("Groq no está configurado")
        if self._client is None:
            self._client = Groq(api_key=api_key, timeout=TIMEOUT_SECONDS)
        return self._client
