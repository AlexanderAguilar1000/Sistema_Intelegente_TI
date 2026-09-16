# Plan 001 — MVP de incidentes: diseño técnico

Implementa `spec.md` de esta misma carpeta. Toda decisión aquí se somete a `docs/constitution.md`.

## 0. Cumplimiento de la constitución
| Principio | Cómo lo cumple este plan |
|---|---|
| 1. Simplicidad del stack | Solo los componentes que hoy lista AGENTS.md: Angular, Spring Boot, FastAPI, PostgreSQL+pgvector, Groq y sentence-transformers para los embeddings. Sin colas, sin caché externa, sin proveedores adicionales. |
| 2. Spec antes que código | Cada módulo, endpoint y test de este plan referencia el RF que implementa (sección 7). |
| 3. Separación lógica/interfaz | Controladores REST y routers FastAPI solo validan entrada y serializan salida; reglas de negocio en servicios de dominio, RAG en el servicio Python. |
| 4. Tests obligatorios | Sección 6: todo RF tiene al menos un test automatizado, con la IA simulada (RNF-9). |
| 5. Persistencia controlada | Un único historial de migraciones Flyway, incluida la extensión `vector` y la tabla de embeddings. |
| 6. Idioma | Identificadores, tablas y campos en inglés; documentación, mensajes de usuario y respuestas de la IA en español. |

## 1. Estructura de módulos

### 1.1 Frontend Angular (`frontend/`) — RF-1, RF-2, RF-6…RF-12, RF-14
Solo presentación: ningún cálculo de estado ni regla de negocio; habilita o deshabilita acciones según lo que devuelve el backend.
- `core/` — interceptor que añade el usuario activo a cada petición, manejo de errores, guardas de ruta según el rol del usuario activo (RF-1), servicio de configuración.
- `user-switcher/` — selector del usuario activo a partir de la lista que devuelve el backend (RF-1).
- `technicians/` — alta de técnico con selector de área (RF-2).
- `incidents/list/` — listado con filtro por prioridad y aviso de lista vacía (RF-8).
- `incidents/detail/` — detalle, acciones de iniciar, analizar, resolver y cerrar sin solución (RF-9, RF-10, RF-11, RF-12, RF-14).
- `incidents/create/` — formulario de registro con aviso de no escribir contraseñas (RF-3, RNF-4).
- `incidents/classification/` — corrección manual y reintento de clasificación (RF-4, RF-6).
- `incidents/assignment/` — asignación y reasignación con lista de técnicos del área (RF-7).
- `shared/` — catálogos recibidos del backend (RF-5), componentes de estado y prioridad.

### 1.2 Backend Spring Boot (`backend/`) — dueño de las reglas de negocio
Capas por módulo: `web` (controlador + DTO) → `service` (reglas) → `domain` (entidades, enums, máquina de estados) → `repository`.
- `users` — lista de usuarios para el selector, resolución del usuario activo a partir de la cabecera `X-User-Id`, comprobación de rol antes de cada caso de uso, alta de técnicos con área obligatoria y consulta de técnicos por área (RF-1, RF-2, RF-7). Sin framework de seguridad: es una comprobación de dominio, no autenticación.
- `catalog` — enums de tipo, prioridad y área; fuente de verdad de los catálogos (RF-5).
- `incidents.domain` — entidad `Incident`, `IncidentStateMachine` con las transiciones de la sección 4, reglas de pertenencia de área, longitud mínima de solución (RF-3, RF-6, RF-7, RF-10, RF-12, RF-14).
- `incidents.service` — casos de uso: registrar, reclasificar, corregir, asignar, listar, iniciar, analizar, resolver, cerrar, reindexar (RF-3, RF-4, RF-6…RF-14).
- `incidents.web` — controladores REST (sección 3.1).
- `ai` — cliente HTTP del servicio Python con sus tiempos límite; traduce fallos a un resultado de dominio, nunca lanza al controlador (RF-4, RF-11, RF-13, RNF-1, RNF-2, RNF-8).
- `audit` — registro de eventos de cada acción (RNF-6).
- `shared` — errores de negocio, respuesta de error homogénea, configuración.

### 1.3 Servicio de IA FastAPI (`ai-service/`) — dueño de embeddings y RAG
- `api/` — routers `classifications`, `analyses`, `embeddings`, `health`: validan el cuerpo y delegan.
- `domain/classification.py` — construcción del prompt y validación contra el catálogo recibido (RF-4).
- `domain/retrieval.py` — embedding de la consulta, búsqueda de los 3 vecinos más cercanos con umbral, exclusión del propio incidente y de los no indexados (RF-11, RF-13).
- `domain/recommendation.py` — prompt RAG con casos similares, recomendación general si no hay casos, validación de que haya al menos un paso con texto (RF-11).
- `domain/prompting.py` — delimitación del texto de usuario como dato, no como instrucción (RNF-5).
- `infrastructure/groq_client.py`; `infrastructure/embedder.py`, que carga una sola vez al arrancar el modelo `paraphrase-multilingual-MiniLM-L12-v2` de sentence-transformers y lo reutiliza en cada petición; `infrastructure/vector_store.py`, única pieza que escribe y consulta `incident_embeddings`.
- `core/` — configuración, claves por variable de entorno (RNF-3), errores tipados.

### 1.4 Migraciones (`backend/src/main/resources/db/migration`) — RF-2, RF-3, RF-13
Un único historial Flyway, ejecutado por Spring Boot al arrancar. `V1__initial_schema.sql` crea la extensión `vector` y las cuatro tablas con sus restricciones e índices; los datos de ejemplo y los cambios posteriores van en migraciones siguientes. El servicio Python no crea ni altera esquema (principio 5).

## 2. Modelo de datos y relaciones

### users — RF-1, RF-2
`id` PK · `username` VARCHAR(50) único · `full_name` VARCHAR(120) · `role` (SUPERVISOR, TECHNICIAN) · `area` (nulo para supervisores) · `created_at`. Sin contraseña: el MVP no autentica (RF-1).
Restricción: `role = 'TECHNICIAN'` exige `area NOT NULL`; `role = 'SUPERVISOR'` exige `area NULL` (RF-2).

### incidents — RF-3…RF-14
`id` PK · `title` VARCHAR(150) · `description` TEXT · `status` (REGISTERED, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED_WITHOUT_SOLUTION) · `classification_status` (PENDING, AI_CLASSIFIED, MANUALLY_CORRECTED) · `type` (BUG, PERFORMANCE, ACCESS, HARDWARE, NETWORK, CONFIGURATION) · `priority` (HIGH, MEDIUM, LOW) · `area` (INFRASTRUCTURE, APPLICATIONS, DATABASE, SECURITY, USER_SUPPORT) · `created_by` FK users · `assigned_to` FK users nulo · `solution` TEXT nulo · `closure_reason` (DUPLICATE, NOT_REPRODUCIBLE, DISCARDED) nulo · `indexed` BOOLEAN · `analysis_started_at` nulo · `created_at`, `assigned_at`, `started_at`, `closed_at` · `version` para bloqueo optimista.

Restricciones que hacen cumplir la spec en la propia base de datos:
- `classification_status = 'PENDING'` ⇒ `type`, `priority` y `area` nulos; en caso contrario, los tres no nulos (RF-4, RF-8).
- `status IN ('ASSIGNED','IN_PROGRESS')` ⇒ `assigned_to` no nulo; `status = 'REGISTERED'` ⇒ `assigned_to` nulo (RF-7, RF-6).
- `status = 'RESOLVED'` ⇒ `solution` no nula con al menos 30 caracteres y `closed_at` no nulo (RF-12).
- `status = 'CLOSED_WITHOUT_SOLUTION'` ⇒ `closure_reason` no nulo y `closed_at` no nulo (RF-14).

Índices: `status`, `priority`, `area`, `assigned_to`, `classification_status` (RF-8).

### incident_events — RNF-6
`id` PK · `incident_id` FK incidents · `action` (CREATED, AI_CLASSIFIED, CLASSIFICATION_FAILED, CLASSIFICATION_RETRIED, MANUALLY_CLASSIFIED, ASSIGNED, REASSIGNED, UNASSIGNED_BY_AREA_CHANGE, STARTED, ANALYSIS_REQUESTED, ANALYSIS_FAILED, RESOLVED, CLOSED_WITHOUT_SOLUTION, INDEXED, INDEXING_FAILED) · `performed_by` FK users nulo cuando actúa el sistema · `performed_at` · `detail` JSONB nulo.

### incident_embeddings — RF-11, RF-13
`incident_id` PK y FK a incidents en cascada · `embedding` vector(384) · `model_name` · `indexed_at`. Índice HNSW con distancia coseno. Solo el servicio Python lee y escribe esta tabla.

### Relaciones
- `users` 1—N `incidents` por `created_by` (RF-3) y 1—N por `assigned_to` (RF-7).
- `incidents` 1—N `incident_events` (RNF-6).
- `incidents` 1—0..1 `incident_embeddings`: solo los resueltos e indexados tienen fila (RF-13).
- No hay tabla de catálogos: son enums en el backend (decisión D4).

## 3. Contratos

### 3.1 API REST Spring Boot ↔ Angular
| Método y ruta | RF |
|---|---|
| `GET /api/users` (lista para el selector de usuario activo) | RF-1 |
| `POST /api/technicians` | RF-2 |
| `GET /api/technicians?area=` | RF-7 |
| `GET /api/catalogs` | RF-5 |
| `POST /api/incidents` | RF-3, RF-4 |
| `GET /api/incidents?priority=&status=&classification=` | RF-8 |
| `GET /api/incidents/{id}` | RF-9 |
| `POST /api/incidents/{id}/classification/retry` | RF-4 |
| `PATCH /api/incidents/{id}/classification` | RF-6 |
| `PUT /api/incidents/{id}/assignment` | RF-7 |
| `POST /api/incidents/{id}/start` | RF-10 |
| `POST /api/incidents/{id}/analysis` | RF-11 |
| `POST /api/incidents/{id}/resolution` | RF-12 |
| `POST /api/incidents/{id}/closure` | RF-14 |
| `POST /api/incidents/{id}/index/retry` | RF-13 |

Toda petición que actúe sobre incidentes lleva la cabecera `X-User-Id` con el usuario activo (RF-1).

Errores homogéneos: `{ "errorCode": "...", "message": "<en español>" }`. 400 si falta o no existe el usuario activo, 403 cuando el rol no permite la acción o el incidente no es del técnico (RF-1, RF-9), 409 para transición inválida o conflicto de versión (RF-7, RF-10, RF-12), 422 para validación (RF-3, RF-12, RF-14).

### 3.2 Contrato Spring Boot → FastAPI
Spring Boot es siempre el cliente; **FastAPI nunca llama al backend**, así que no hay dependencia circular ni devolución asíncrona de la clasificación. Todas las respuestas de error usan `{"error_code": "...", "detail": "..."}`.

**a) `POST /ai/v1/classifications` — RF-4, RNF-1, RNF-5**
```json
petición  { "incident_id": 42, "title": "...", "description": "...",
            "catalog": { "types": ["BUG", "..."], "priorities": ["HIGH", "..."], "areas": ["APPLICATIONS", "..."] } }
200       { "type": "BUG", "priority": "HIGH", "area": "APPLICATIONS", "model": "...", "elapsed_ms": 1840 }
422       { "error_code": "INVALID_CLASSIFICATION", "detail": "valor fuera de catálogo" }
503       { "error_code": "AI_UNAVAILABLE", "detail": "..." }
```
Tiempo límite del cliente: 10 s (RNF-1). Cualquier 4xx, 5xx o agotamiento del plazo ⇒ el incidente queda `PENDING` (RF-3).

**b) `POST /ai/v1/analyses` — RF-11, RNF-2, RNF-5**
```json
petición  { "incident_id": 42, "title": "...", "description": "...", "type": "BUG",
            "priority": "HIGH", "area": "APPLICATIONS", "top_k": 3, "min_score": 0.75 }
200       { "grounded": true,
            "steps": ["Revisar ...", "Comprobar ..."],
            "references": [ { "incident_id": 7, "title": "...", "score": 0.83 } ],
            "model": "...", "elapsed_ms": 9120 }
200       { "grounded": false, "steps": ["..."], "references": [], "model": "...", "elapsed_ms": 4100 }
422       { "error_code": "INVALID_RECOMMENDATION", "detail": "sin pasos con texto" }
503       { "error_code": "AI_UNAVAILABLE", "detail": "..." }
```
Tiempo límite: 30 s (RNF-2). `grounded: false` es el caso "sin similares" (RF-11), no un error. La búsqueda excluye el propio incidente, los no resueltos y los no indexados (RF-13).

El servicio Python genera el embedding del incidente actual, lo usa para buscar en pgvector y envía a Groq **el texto** de los casos recuperados con sus soluciones. Los vectores nunca salen del servicio Python ni llegan al modelo de lenguaje.

**c) `POST /ai/v1/embeddings` — RF-13**
```json
petición  { "incident_id": 42, "title": "...", "description": "...", "solution": "...", "type": "BUG", "area": "APPLICATIONS" }
200       { "indexed": true, "model": "...", "dimensions": 384 }
503       { "error_code": "AI_UNAVAILABLE", "detail": "..." }
```
Idempotente: reescribe la fila si ya existe, así el reintento del supervisor es seguro. Si falla, Spring Boot deja `indexed = false` y el incidente sigue Resuelto (RF-13, RNF-8).

**d) `GET /ai/v1/health`** — comprobación de arranque.

### 3.3 Reglas transversales del contrato
- JSON UTF-8; todos los textos para el usuario en español (RNF-7).
- Cabecera `X-Request-Id` propagada desde el backend para correlacionar trazas (RNF-6).
- Cabecera `X-Service-Token` con clave compartida por variable de entorno; la clave de Groq vive solo en el servicio Python (RNF-3).
- El servicio Python jamás recibe datos de cuentas de usuario (RNF-4) y delimita el texto recibido como dato (RNF-5).

## 4. Máquina de estados (RF-3, RF-6, RF-7, RF-10, RF-12, RF-14)
```
REGISTERED --asignar(técnico del área, incidente clasificado)--> ASSIGNED
ASSIGNED --iniciar(técnico asignado)--> IN_PROGRESS
ASSIGNED|IN_PROGRESS --reasignar(supervisor)--> ASSIGNED
ASSIGNED|IN_PROGRESS --cambio de área que excluye al técnico--> REGISTERED (sin técnico)
IN_PROGRESS --resolver(solución >= 30 caracteres)--> RESOLVED --> indexación (RF-13)
ASSIGNED|IN_PROGRESS --cerrar sin solución(motivo)--> CLOSED_WITHOUT_SOLUTION
RESOLVED y CLOSED_WITHOUT_SOLUTION son finales.
```
Toda transición pasa por `IncidentStateMachine`; los controladores no deciden (principio 3).

## 5. Decisiones técnicas justificadas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D1 | Clasificación **síncrona** en el alta, con plazo de 10 s en el cliente HTTP de Spring Boot (RF-3, RNF-1) | El supervisor ve el resultado en la misma pantalla y no hace falta infraestructura adicional | Clasificación en segundo plano con cola o hilos: exige componentes fuera del stack (principio 1) y complica los tests |
| D2 | El servicio Python es el **único dueño** de `incident_embeddings`; el backend nunca consulta vectores (RF-11, RF-13) | Lo exige AGENTS.md y mantiene el RAG en un solo sitio (principio 3) | Búsqueda vectorial con SQL nativo desde Spring Boot: duplicaría la lógica de similitud en dos servicios |
| D3 | **Un solo historial de migraciones** Flyway desde Spring Boot, incluida `CREATE EXTENSION vector` y la tabla de embeddings (principio 5) | Un único orden de cambios sobre un único esquema, reproducible al arrancar | Alembic en el servicio Python: dos historiales sobre la misma base, con riesgo de orden incompatible |
| D4 | Catálogos como **enums en el backend**, expuestos por `GET /api/catalogs` y enviados en cada petición de clasificación (RF-5) | Una sola fuente de verdad; el servicio Python valida contra lo que recibe, sin duplicar listas | Tablas de catálogo en base de datos (innecesario: son fijos) y listas duplicadas en Python (divergencia silenciosa) |
| D5 | Embeddings con **sentence-transformers** y el modelo `paraphrase-multilingual-MiniLM-L12-v2` (384 dimensiones) dentro de FastAPI (RF-13) | Groq no ofrece embeddings; el modelo es multilingüe, entiende bien el español de los incidentes, corre en CPU y no añade proveedor de pago ni saca más texto fuera | API de embeddings externa: proveedor nuevo, coste por uso y más datos a terceros. Modelos solo en inglés (`all-MiniLM-L6-v2`): más ligeros, pero encuentran bastantes menos casos con texto en español |
| D6 | **Angular** como frontend separado, con Spring Boot sirviendo solo la API (aprobado explícitamente por el usuario) | Es la tecnología elegida para la interfaz; deja la separación interfaz/lógica muy marcada (principio 3) | Plantillas Thymeleaf en el propio backend: un solo despliegue y sin stack nuevo, pero descartada por decisión del usuario |
| D7 | **Sin autenticación**: el usuario activo se elige en el frontend y viaja en la cabecera `X-User-Id`; el backend resuelve ese usuario y aplica las reglas de rol y de área como reglas de dominio (RF-1) | Decisión explícita del usuario para el MVP: mantiene intactas todas las reglas de rol sin implementar seguridad, y deja el sistema listo para añadir autenticación real después sin tocar los casos de uso | Sesión con cookie y Spring Security, y JWT: ambas descartadas por el usuario por tratarse de un MVP; añadían gestión de credenciales, caducidad y renovación sin aportar nada a lo que se quiere demostrar |
| D8 | **Bloqueo optimista** con columna `version` en incidents (casos límite de concurrencia) | Resuelve la doble asignación y la resolución tardía del técnico anterior devolviendo 409, sin bloquear filas | Bloqueo pesimista: contención innecesaria para un volumen bajo |
| D9 | Marca `analysis_started_at` con caducidad igual al plazo de análisis para impedir análisis simultáneos (RF-11) | Sobrevive a reinicios y no depende de memoria del proceso | Candado en memoria: se pierde al reiniciar y falla si algún día hay más de una instancia |
| D10 | Respuestas de Groq en **JSON estructurado**, validadas en Python antes de contestar (RF-4, RF-11) | Permite rechazar valores fuera de catálogo y recomendaciones vacías antes de que lleguen al usuario | Interpretar texto libre: no verificable y frágil ante cambios del modelo |
| D11 | Fallos de IA traducidos a **resultados de dominio**, nunca a excepciones que suban al controlador (RNF-8) | Garantiza que un incidente o una solución nunca se pierdan por un fallo externo | Propagar la excepción: convertiría un fallo de Groq en un error 500 y en pérdida de datos del formulario |
| D12 | Tests de integración contra la **instalación local de PostgreSQL+pgvector**, en una base de datos separada de la de desarrollo (por ejemplo `ti_incidentes_test`), con la conexión por variables de entorno y limpieza de datos en cada prueba (principio 4) | Es el único modo de probar de verdad las restricciones, las migraciones y la búsqueda vectorial, y no añade ninguna dependencia nueva al entorno del desarrollador | Base en memoria H2: no soporta pgvector ni los tipos usados. Docker o Testcontainers: descartados porque el MVP no debe depender de contenedores, y Testcontainers sería además una librería nueva |
| D13 | El servicio Python **no llama nunca** al backend (sección 3.2) | Evita dependencia circular y deja un solo sentido de comunicación, más fácil de simular en tests | Devolución asíncrona de la clasificación desde Python hacia Spring Boot, como se planteó en la idea inicial: obliga a exponer un endpoint de retorno y a gestionar reintentos |

## 6. Estrategia de test

**Backend — `./mvnw test`**
1. Dominio, sin infraestructura: todas las transiciones válidas e inválidas de la máquina de estados (RF-3, RF-7, RF-10, RF-12, RF-14); pertenencia de área en la asignación (RF-7); longitud mínima de solución (RF-12); reglas de corrección manual y retirada de asignación por cambio de área (RF-6).
2. Servicios con **cliente de IA simulado** (RNF-9): plazo agotado ⇒ incidente pendiente (RF-3, RNF-1); clasificación fuera de catálogo ⇒ pendiente (RF-4); reintento exitoso ⇒ clasificado (RF-4); indexación fallida ⇒ `indexed = false` y el incidente sigue Resuelto (RF-13, RNF-8); segundo análisis rechazado mientras hay uno en curso (RF-11).
3. Web con MockMvc, sobre el usuario activo de la cabecera: petición sin `X-User-Id` ⇒ 400 (RF-1); matriz de rol y endpoint (RF-1); técnico que abre un incidente ajeno ⇒ 403 (RF-9); supervisor que intenta analizar ⇒ 403 (RF-11); asignación a técnico de otra área ⇒ 409 con motivo (RF-7).
4. Persistencia contra PostgreSQL real: migraciones aplicadas, restricciones que rechazan estados imposibles (RF-4, RF-7, RF-12, RF-14), filtros del listado por rol y prioridad y exclusión de pendientes (RF-8), conflicto de versión en asignación concurrente (casos límite).
5. Contrato del cliente de IA contra un servidor HTTP simulado: 200, 422, 503 y plazo agotado para los tres endpoints de la sección 3.2.

**Servicio de IA — `pytest`**
1. Validación de catálogo y parseo de la respuesta de Groq, con cliente de Groq simulado (RF-4).
2. Validación de la recomendación: sin pasos con texto ⇒ 422 (RF-11).
3. Construcción del prompt: el texto de usuario aparece delimitado y con la instrucción de no obedecer lo que contenga (RNF-5); no se incluye ningún dato de cuenta (RNF-4).
4. Recuperación contra PostgreSQL+pgvector con datos sembrados y embeddings deterministas simulados: devuelve como mucho 3 casos, descarta los que no superan el umbral, excluye el propio incidente, los no resueltos y los no indexados (RF-11, RF-13).
5. API con el cliente de pruebas de FastAPI: forma exacta de las respuestas y de los errores de los tres endpoints; `/embeddings` aplicado dos veces deja una sola fila (RF-13).

**Frontend Angular — pruebas unitarias del propio framework**
Guardas de ruta por rol (RF-1), filtro de prioridad (RF-8) y visibilidad de acciones según estado y rol, incluida la ausencia de "Analizar con IA" para el supervisor (RF-9, RF-11).

**Manual, en la demostración (criterios de la sección 8 de la spec)**
Flujo completo de punta a punta y los tres caminos de error de la IA. Además, un guion aparte, ejecutado a mano y nunca en la suite, que llama a Groq de verdad para comprobar que sigue respondiendo en el formato esperado (RNF-9).

Regla de cierre: ninguna tarea se da por terminada con `./mvnw test` o `pytest` en rojo (principio 4).

## 7. Matriz de cobertura RF → diseño
| RF | Módulos | Contrato | Tests |
|---|---|---|---|
| RF-1 Usuario activo y roles | `users`, `core/` y `user-switcher/` Angular | `GET /api/users`, cabecera `X-User-Id` | Backend 3, Angular |
| RF-2 Alta de técnicos | `users`, `technicians/` | `POST /api/technicians` | Backend 1, 4 |
| RF-3 Registro síncrono | `incidents.service`, `ai` | `POST /api/incidents` → `/ai/v1/classifications` | Backend 1, 2, 5 |
| RF-4 Clasificación con IA | `ai`, `domain/classification.py` | `/ai/v1/classifications` | Backend 2, 5; IA 1 |
| RF-5 Catálogos | `catalog`, `shared/` | `GET /api/catalogs` | Backend 1; IA 1 |
| RF-6 Corrección manual | `incidents.domain`, `incidents/classification/` | `PATCH .../classification` | Backend 1, 4 |
| RF-7 Asignación y reasignación | `incidents.domain`, `users`, `incidents/assignment/` | `PUT .../assignment`, `GET /api/technicians?area=` | Backend 1, 3, 4 |
| RF-8 Listado y filtrado | `incidents.service`, `incidents/list/` | `GET /api/incidents` | Backend 4; Angular |
| RF-9 Detalle | `incidents.web`, `incidents/detail/` | `GET /api/incidents/{id}` | Backend 3; Angular |
| RF-10 Inicio del trabajo | `incidents.domain` | `POST .../start` | Backend 1 |
| RF-11 Analizar con IA | `ai`, `domain/retrieval.py`, `domain/recommendation.py` | `/ai/v1/analyses` | Backend 2, 3, 5; IA 2, 4, 5 |
| RF-12 Resolución | `incidents.domain` | `POST .../resolution` | Backend 1, 4 |
| RF-13 Histórico e indexación | `ai`, `infrastructure/vector_store.py` | `/ai/v1/embeddings`, `POST .../index/retry` | Backend 2, 5; IA 4, 5 |
| RF-14 Cierre sin solución | `incidents.domain` | `POST .../closure` | Backend 1, 4 |

## 8. Orden de implementación sugerido
1. Migraciones y modelo de datos (RF-2, RF-3, RF-13) · 2. Usuarios, selector de usuario activo y reglas de rol (RF-1) · 3. Alta de técnicos y catálogos (RF-2, RF-5) · 4. Registro con IA simulada y máquina de estados (RF-3, RF-6, RF-7, RF-10, RF-12, RF-14) · 5. Servicio Python de clasificación (RF-4) · 6. Indexación y recuperación (RF-13, RF-11) · 7. Frontend Angular (RF-8, RF-9 y el resto de pantallas) · 8. Demostración de punta a punta.

## 9. Riesgos y pendientes
- **Deuda consciente:** sin autenticación, cualquiera que llegue a la API puede actuar como otro usuario cambiando la cabecera `X-User-Id`. Es aceptable para un MVP local de demostración, pero añadir autenticación real es el primer paso antes de exponer el sistema a usuarios reales.
- **Requisito previo del entorno:** la instalación local de PostgreSQL debe tener la extensión `pgvector` disponible y habilitada, y una base de datos de test creada aparte de la de desarrollo. Sin eso fallan las migraciones y todas las pruebas de búsqueda vectorial.
- El umbral de parecido (0,75 propuesto) se calibra con datos reales; hasta entonces puede dar demasiados o ningún caso similar (duda abierta de la spec).
- La dimensión 384 queda atada a `paraphrase-multilingual-MiniLM-L12-v2`: cambiar de modelo obliga a una migración y a reindexar todo el histórico (principio 5).
- El modelo se descarga la primera vez que arranca el servicio (unos 470 MB) y esa primera ejecución tarda notablemente más. Conviene fijar la versión del modelo y de sentence-transformers para que todos usen el mismo y los vectores sean comparables.
- Con el histórico vacío, las primeras demostraciones darán siempre recomendación general; conviene resolver varios incidentes antes de enseñar el RAG.
- Las diez dudas menores de la sección 9 de la spec siguen abiertas y no bloquean este plan.
- AGENTS.md ya recoge Angular, el modelo local de embeddings y la ausencia de autenticación, así que el stack de este plan es el aprobado (principio 1).
