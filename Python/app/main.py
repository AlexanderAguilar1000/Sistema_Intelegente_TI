from collections.abc import AsyncIterator, Callable
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.classifications import router as classifications_router
from app.api.health import router as health_router
from app.clients.embedder import Embedder, EmbedderInfo
from app.clients.groq_client import GroqClient, LlmClient
from app.core.errors import AiUnavailableError, InvalidClassificationError

load_dotenv()


def _error(status: int, code: str, detail: str) -> JSONResponse:
    return JSONResponse(
        status_code=status, content={"error_code": code, "detail": detail}
    )


# PRacticar esto de aqui porque es sincrono dentro de asincro , como es que guarda dentro de memoria
def create_app(
    embedder_loader: Callable[
        [], EmbedderInfo
    ] = Embedder.load,  # Embedder_loader lo que hace es dice que este es una función
    # El cual recibe 0 paraemteo y devuelve embeder info  y carga Embeder.load
    # y este devuelve el modelo de hugging face entonces esto devuelve
    # el nombre del modelo y las dimensiones de este modelo
    groq_client: LlmClient | None = None,
) -> FastAPI:
    """Crea la aplicación; el modelo se carga una sola vez, al arrancar."""  # claro el modelo de IA se carga una sola vez no cada vez que se hace una llamada HTTP

    @asynccontextmanager  # Esta es una función asincrona entonces se va a cargar el modelo de IA , pero mientras carga se va a guardar las rutas que esta en la linea 23
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:#Esto crea un objeto app: FastAPI 
        app.state.embedder = ( 
            embedder_loader()
        )  # entonces aqui llamo a la función embedder_loader para cargar el modelo
        app.state.groq = groq_client or GroqClient()
        yield

    app = FastAPI(title="Servicio de IA", lifespan=lifespan)
    app.include_router(
        health_router
    )  # guarda la ruta del enpoind que se encuentra en health.py
    app.include_router(classifications_router)

    @app.exception_handler(InvalidClassificationError)
    async def invalid_classification(
        _: Request, error: InvalidClassificationError
    ) -> JSONResponse:
        return _error(422, "INVALID_CLASSIFICATION", str(error))

    @app.exception_handler(AiUnavailableError)
    async def ai_unavailable(_: Request, error: AiUnavailableError) -> JSONResponse:
        return _error(503, "AI_UNAVAILABLE", str(error))

    return app


app = create_app()
