# AGENTS.md — TI_SISTEMA_INTELIGENT

## Proyecto
Sistema de gestión y resolución de incidentes de TI que permite registrar, clasificar, asignar, diagnosticar y resolver incidentes, conservando las soluciones históricas como conocimiento reutilizable. La arquitectura combina Angular para la interfaz de usuario, Spring Boot para la lógica de negocio y la API REST, Python + FastAPI para las funcionalidades de IA, PostgreSQL + pgvector para persistencia y búsqueda semántica, y Groq como proveedor del LLM. El sistema aplica un enfoque RAG: genera los embeddings con sentence-transformers dentro del servicio Python, recupera desde pgvector los incidentes y soluciones históricas más parecidos, y envía ese texto a Groq para que redacte la recomendación de diagnóstico para el técnico.

## Comandos
- Base de datos: instancia local de PostgreSQL con la extensión `pgvector` habilitada, administrada con pgAdmin (sin contenedores).
- Ejecutar backend (Spring Boot): `./mvnw spring-boot:run`
- Ejecutar servicio de IA (FastAPI): `uvicorn app.main:app --reload --port 8001`
- Ejecutar frontend (Angular, en `frontend/`): `npm start`
- Tests backend: `./mvnw test`
- Tests servicio de IA: `pytest`
- Tests frontend: `npm test`
- Lint/formato backend: `./mvnw spotless:apply`
- Lint/formato servicio de IA: `ruff check . && ruff format .`
- Lint/formato frontend: `npm run lint`

## Estilo y convenciones
- Backend: Java 17+, Spring Boot 3.x. Clases en PascalCase, variables y métodos en camelCase, paquetes en minúsculas.
- Servicio de IA: Python 3.11+, FastAPI, PEP8, type hints obligatorios en todas las funciones públicas, Pydantic para modelos de entrada/salida.
- Frontend: Angular 17+ con componentes standalone, TypeScript en modo estricto. Clases y componentes en PascalCase, métodos y propiedades en camelCase, archivos en kebab-case.
- Identificadores de código (clases, variables, funciones) en inglés. Comentarios, mensajes de commit y documentación en español.
- Los embeddings y búsquedas semánticas se manejan solo en el servicio Python contra pgvector; el backend Spring Boot no accede directamente a los vectores.

## Reglas
- Lee docs/constitution.md y la spec activa antes de tocar código.
- No modificar el esquema de PostgreSQL/pgvector (tablas, dimensiones de embeddings, índices) sin crear la migración correspondiente.
- No mezclar responsabilidades entre servicios: la lógica de negocio y la API REST viven en Spring Boot; el procesamiento de IA, RAG y embeddings vive en FastAPI.
- El frontend Angular es solo presentación: no contiene reglas de negocio ni decide estados; refleja lo que devuelve la API.
- Los embeddings se generan con sentence-transformers y el modelo `paraphrase-multilingual-MiniLM-L12-v2` (384 dimensiones) dentro del servicio Python. Cambiar de modelo obliga a una migración y a reindexar todo el histórico, así que se consulta antes.
- A Groq se le envía el texto de los incidentes y soluciones recuperados, nunca los vectores: el embedding solo sirve para buscar en pgvector.
- El MVP no implementa autenticación ni gestión de sesiones: el usuario activo se elige en el frontend y viaja en la cabecera `X-User-Id`. No introducir Spring Security, JWT ni contraseñas sin aprobación previa.
- No introducir Docker ni contenedores como dependencia del MVP: se trabaja contra la instalación local de PostgreSQL.
- No añadir nuevas dependencias, librerías o frameworks sin confirmar antes con el usuario.
- No exponer claves de API (Groq, credenciales de base de datos, etc.) en código ni en commits; usar siempre variables de entorno.
- No cambiar el proveedor del LLM (Groq) ni el motor de búsqueda semántica (pgvector) sin discutirlo primero con el usuario.


