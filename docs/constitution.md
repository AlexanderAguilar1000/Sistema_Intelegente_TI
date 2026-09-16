# Constitution — TI_SISTEMA_INTELIGENT

1. Simplicidad del stack: solo se usan los componentes de AGENTS.md (Spring Boot, FastAPI, PostgreSQL+pgvector, Groq); ningún framework/librería/servicio nuevo sin aprobación explícita.
2. Spec antes que código: ninguna funcionalidad se implementa sin una spec aprobada que la describa; todo cambio referencia la spec que implementa.
3. Separación lógica/interfaz: la lógica de negocio y RAG vive en los servicios; controladores/endpoints solo orquestan entrada/salida, sin reglas de negocio embebidas.
4. Tests obligatorios: ninguna tarea se cierra si `./mvnw test` o `pytest` fallan; toda funcionalidad nueva incluye al menos un test que la cubra.
5. Persistencia controlada: todo cambio de esquema en PostgreSQL/pgvector pasa por una migración versionada; nunca se modifica el esquema directamente.
6. Idioma: identificadores de código en inglés; comentarios, mensajes de commit, specs y documentación en español.
