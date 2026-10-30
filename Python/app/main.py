from collections.abc import AsyncIterator, Callable
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.health import router as health_router
from app.infrastructure.embedder import Embedder, EmbedderInfo

#PRacticar esto de aqui porque es sincrono dentro de asincro , como es que guarda dentro de memoria 
def create_app(
    embedder_loader: Callable[[], EmbedderInfo] = Embedder.load,  # Embedder_loader lo que hace es dice que este es una función 
                                                                  # El cual recibe 0 paraemteo y devuelve embeder info  y carga Embeder.load 
                                                                  # y este devuelve el modelo de hugging face entonces esto devuelve 
                                                                  #el nombre del modelo y las dimensiones de este modelo
) -> FastAPI:
    """Crea la aplicación; el modelo se carga una sola vez, al arrancar."""#claro el modelo de IA se carga una sola vez no cada vez que se hace una llamada HTTP

    @asynccontextmanager #Esta es una función asincrona entonces se va a cargar el modelo de IA , pero mientras carga se va a guardar las rutas que esta en la linea 23
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        app.state.embedder = embedder_loader()#entonces aqui llamo a la función embedder_loader para cargar el modelo 
        yield

    app = FastAPI(title="Servicio de IA", lifespan=lifespan)
    app.include_router(health_router)#guarda la ruta del enpoind que se encuentra en health.py 
    return app


app = create_app()
