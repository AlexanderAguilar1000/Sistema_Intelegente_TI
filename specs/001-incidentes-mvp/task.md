# Tareas 001 — MVP de incidentes

Desglose ejecutable de `spec.md` y `plan.md`. Cada tarea es una porción de funcionalidad completa y demostrable de 20-30 minutos, no un trozo de capa técnica.

**Reglas de trabajo**
- Se abordan en orden: ninguna tarea depende de otra que aparezca más abajo.
- Una tarea solo se marca cuando su línea **Hecho cuando** se cumple y `./mvnw test` o `pytest` están en verde (principio 4 de la constitución).
- El backend trabaja contra la instalación local de PostgreSQL con `pgvector`; no hay contenedores.

---

## Bloque 0 — Cimientos

- [ ] **T-01 · Arrancar el backend con el esquema creado** — RF-2, RF-3, RF-13
  **Hecho cuando:** `./mvnw spring-boot:run` arranca sin errores contra la base local y en pgAdmin existen las tablas `users`, `incidents`, `incident_events` e `incident_embeddings`, con la extensión `vector` habilitada.

- [ ] **T-02 · Precargar supervisores y técnicos de ejemplo** — RF-1, RF-2
  **Hecho cuando:** al arrancar de cero, la tabla `users` contiene al menos dos supervisores y cuatro técnicos repartidos en áreas distintas, y volver a arrancar no los duplica.

- [ ] **T-03 · Publicar los catálogos de tipo, prioridad y área** — RF-5
  **Hecho cuando:** `GET /api/catalogs` devuelve las tres listas con exactamente los valores de RF-5, y un test falla si alguien añade o quita un valor.

- [ ] **T-04 · Mostrar la lista de usuarios para el selector** — RF-1
  **Hecho cuando:** `GET /api/users` devuelve nombre, rol y área de cada usuario precargado, con un test que lo comprueba.

## Bloque 1 — Usuario activo y técnicos

- [ ] **T-05 · Identificar al usuario activo en cada petición** — RF-1
  **Hecho cuando:** una petición a incidentes sin cabecera `X-User-Id` devuelve 400, con un id inexistente también 400, y con un id válido la petición se atribuye a ese usuario; tests de los tres casos.

- [ ] **T-06 · Denegar acciones que no corresponden al rol** — RF-1
  **Hecho cuando:** un técnico que llama a una acción de supervisor recibe 403 con mensaje en español, y un test cubre al menos dos endpoints de supervisor.

- [ ] **T-07 · Dar de alta un técnico con su área** — RF-2
  **Hecho cuando:** `POST /api/technicians` crea el técnico con rol y área; usuario repetido, área fuera de catálogo o campo faltante devuelven error indicando la causa; tests de los cuatro casos.

- [ ] **T-08 · Mostrar la lista de técnicos de un área** — RF-7
  **Hecho cuando:** `GET /api/technicians?area=APPLICATIONS` devuelve solo los técnicos de esa área y ninguno de otra, comprobado con técnicos sembrados en dos áreas distintas.

## Bloque 2 — Incidentes sin IA

- [ ] **T-09 · Registrar un incidente** — RF-3
  **Hecho cuando:** `POST /api/incidents` guarda el incidente en estado Registrado y pendiente de clasificación, con fecha y supervisor; título o descripción vacíos devuelven 422 indicando el campo; tests.

- [ ] **T-10 · Fijar las transiciones válidas del incidente** — RF-3, RF-7, RF-10, RF-12, RF-14
  **Hecho cuando:** un test unitario recorre las transiciones de la sección 4 del plan, acepta todas las válidas y rechaza las inválidas (resolver sin iniciar, actuar sobre un cerrado, etc.).

- [ ] **T-11 · Listar incidentes según quién mira** — RF-8
  **Hecho cuando:** el supervisor recibe todos los incidentes y el técnico solo los suyos, en la misma llamada `GET /api/incidents`; test con dos técnicos y un supervisor.

- [ ] **T-12 · Filtrar el listado por prioridad** — RF-8
  **Hecho cuando:** `GET /api/incidents?priority=HIGH` devuelve solo los de prioridad alta dentro de los visibles, los pendientes de clasificación quedan fuera del filtro, y sin resultados se indica que la lista está vacía; tests.

- [ ] **T-13 · Ver el detalle de un incidente** — RF-9
  **Hecho cuando:** `GET /api/incidents/{id}` devuelve título, descripción, clasificación, estado, fechas y solución o motivo de cierre; un técnico que pide un incidente ajeno recibe 403 y el supervisor puede ver cualquiera; tests.

- [ ] **T-14 · Clasificar un incidente a mano** — RF-6
  **Hecho cuando:** `PATCH /api/incidents/{id}/classification` fija tipo, prioridad y área con valores de catálogo, quita la marca de pendiente y deja la clasificación como corregida manualmente; sobre un incidente cerrado devuelve 409; tests.

- [ ] **T-15 · Asignar un incidente a un técnico de su área** — RF-7
  **Hecho cuando:** `PUT /api/incidents/{id}/assignment` pasa el incidente a Asignado y registra técnico, fecha y supervisor; un técnico de otra área se rechaza con motivo, y un incidente pendiente de clasificación no se puede asignar; tests de los tres casos.

- [ ] **T-16 · Reasignar y retirar la asignación al cambiar el área** — RF-6, RF-7
  **Hecho cuando:** reasignar a otro técnico del área deja el incidente en Asignado con el nuevo responsable; cambiar el área a una que excluye al técnico actual lo devuelve a Registrado sin técnico; el técnico anterior que intenta actuar recibe 403; tests.

- [ ] **T-17 · Iniciar el trabajo de un incidente** — RF-10
  **Hecho cuando:** `POST /api/incidents/{id}/start` pasa a En progreso solo si lo pide el técnico asignado; otro técnico recibe 403 y un estado incorrecto 409; tests.

- [ ] **T-18 · Resolver un incidente con su solución** — RF-12
  **Hecho cuando:** `POST /api/incidents/{id}/resolution` con 30 caracteres o más pasa a Resuelto y guarda solución, fecha y técnico; con menos de 30 devuelve 422; si no está En progreso devuelve 409; tests.

- [ ] **T-19 · Cerrar un incidente sin solución** — RF-14
  **Hecho cuando:** `POST /api/incidents/{id}/closure` con motivo (duplicado, no reproducible o descartado) pasa a Cerrado sin solución y no exige texto de solución; sin motivo devuelve 422; tests.

- [ ] **T-20 · Bloquear cambios sobre incidentes cerrados** — RF-12, RF-14
  **Hecho cuando:** corregir la clasificación, reasignar, iniciar, resolver o cerrar un incidente ya cerrado devuelve 409 en todos los casos; test parametrizado sobre los dos estados finales.

- [ ] **T-21 · Resolver los choques entre dos acciones simultáneas** — RF-7, RF-12
  **Hecho cuando:** dos asignaciones concurrentes sobre el mismo incidente dejan una sola ganadora y la otra recibe 409; test que simula el conflicto de versión.

- [ ] **T-22 · Dejar traza de quién hace cada cosa** — RNF-6
  **Hecho cuando:** un test recorre el ciclo completo (registrar, clasificar a mano, asignar, reasignar, iniciar, resolver) y encuentra en `incident_events` un evento por acción con su usuario y su fecha.

## Bloque 3 — Servicio de IA

- [ ] **T-23 · Arrancar el servicio de IA con el modelo cargado** — RF-4, RF-13
  **Hecho cuando:** `uvicorn app.main:app --port 8001` arranca, el modelo `paraphrase-multilingual-MiniLM-L12-v2` queda cargado una sola vez al inicio y `GET /ai/v1/health` responde indicando el modelo en uso; `pytest` pasa con al menos ese test.

- [ ] **T-24 · Clasificar un incidente con Groq** — RF-4
  **Hecho cuando:** `POST /ai/v1/classifications` devuelve tipo, prioridad y área del catálogo recibido; si Groq responde un valor fuera de catálogo devuelve 422 y si no responde devuelve 503; pytest con Groq simulado en los tres casos.

- [ ] **T-25 · Blindar los prompts frente al texto del usuario** — RNF-4, RNF-5
  **Hecho cuando:** un test comprueba que el texto del incidente viaja delimitado y acompañado de la instrucción de no obedecer lo que contenga, y que en el prompt no aparece ningún dato de cuentas de usuario.

- [ ] **T-26 · Indexar un incidente resuelto** — RF-13
  **Hecho cuando:** `POST /ai/v1/embeddings` genera con sentence-transformers un vector de 384 dimensiones a partir del título, la descripción y la solución, y deja la fila en `incident_embeddings`; llamarlo dos veces sobre el mismo incidente deja una sola fila actualizada; pytest.

- [ ] **T-27 · Encontrar los incidentes más parecidos** — RF-11, RF-13
  **Hecho cuando:** con incidentes sembrados, la búsqueda devuelve como mucho 3 casos, descarta los que no superan el umbral y excluye el propio incidente, los no resueltos y los no indexados; pytest con embeddings deterministas.

- [ ] **T-28 · Generar la recomendación paso a paso** — RF-11
  **Hecho cuando:** `POST /ai/v1/analyses` devuelve pasos y referencias cuando hay casos similares, devuelve recomendación general avisando de que no hay casos previos cuando no los hay, y 422 si Groq responde sin ningún paso con texto; pytest de los tres caminos.

## Bloque 4 — Integración backend ↔ IA

- [ ] **T-29 · Hablar con el servicio de IA sin romperse** — RF-4, RF-11, RF-13, RNF-1, RNF-2, RNF-8
  **Hecho cuando:** un test de contrato contra un servidor simulado cubre 200, 422, 503 y plazo agotado en los tres endpoints, y en ningún caso sube una excepción al controlador.

- [ ] **T-30 · Clasificar el incidente al registrarlo** — RF-3, RF-4, RNF-1
  **Hecho cuando:** con la IA simulada respondiendo bien, el incidente queda clasificado en el mismo registro; si tarda más de 10 segundos o responde algo inválido, el incidente se guarda igualmente marcado como pendiente; tests de ambos caminos.

- [ ] **T-31 · Reintentar la clasificación de un pendiente** — RF-4
  **Hecho cuando:** `POST /api/incidents/{id}/classification/retry` deja el incidente clasificado si la IA responde bien y lo deja pendiente si vuelve a fallar; solo lo puede lanzar el supervisor; tests.

- [ ] **T-32 · Incorporar al histórico el incidente resuelto** — RF-13
  **Hecho cuando:** al resolver se pide la indexación y el incidente queda marcado como indexado; si la indexación falla, sigue Resuelto pero marcado sin indexar y no se pierde la solución; tests.

- [ ] **T-33 · Reindexar un incidente que quedó fuera del histórico** — RF-13
  **Hecho cuando:** `POST /api/incidents/{id}/index/retry` lo marca como indexado; mientras esté sin indexar no aparece como caso similar; tests.

- [ ] **T-34 · Analizar con IA desde el backend** — RF-11
  **Hecho cuando:** `POST /api/incidents/{id}/analysis` devuelve pasos y referencias al técnico asignado; el supervisor recibe 403; un segundo análisis mientras hay uno en curso recibe 409; un fallo de la IA devuelve error sin alterar el incidente; tests de los cuatro casos.

## Bloque 5 — Frontend Angular

- [ ] **T-35 · Elegir con qué usuario se trabaja** — RF-1
  **Hecho cuando:** en el navegador se puede seleccionar un usuario de la lista, el menú cambia según su rol, y todas las peticiones salen con su `X-User-Id`; cambiar de usuario actualiza lo que se ve sin recargar.

- [ ] **T-36 · Ver y filtrar los incidentes** — RF-8
  **Hecho cuando:** el supervisor ve todos los incidentes y el técnico solo los suyos, el filtro por prioridad funciona en pantalla y una lista sin resultados muestra el aviso correspondiente.

- [ ] **T-37 · Registrar un incidente desde la interfaz** — RF-3, RNF-4
  **Hecho cuando:** el formulario crea el incidente y muestra su clasificación, o el aviso de pendiente si la IA no respondió; el formulario advierte de no escribir contraseñas.

- [ ] **T-38 · Abrir el detalle de un incidente** — RF-9
  **Hecho cuando:** el detalle muestra todos los datos del incidente, y el supervisor lo ve sin el botón "Analizar con IA".

- [ ] **T-39 · Dar de alta un técnico desde la interfaz** — RF-2
  **Hecho cuando:** el supervisor crea un técnico eligiendo su área y ese técnico aparece después en el desplegable de asignación de esa área.

- [ ] **T-40 · Corregir la clasificación desde la interfaz** — RF-4, RF-6
  **Hecho cuando:** el supervisor puede cambiar tipo, prioridad y área eligiendo del catálogo, y lanzar el reintento de clasificación en un incidente pendiente, viendo el resultado en pantalla.

- [ ] **T-41 · Asignar y reasignar desde la interfaz** — RF-7
  **Hecho cuando:** el desplegable de técnicos solo ofrece los del área del incidente, la asignación se refleja en el listado, y un incidente pendiente de clasificación no ofrece la acción de asignar.

- [ ] **T-42 · Trabajar el incidente como técnico** — RF-10, RF-12, RF-14
  **Hecho cuando:** el técnico puede iniciar, resolver escribiendo la solución y cerrar sin solución eligiendo motivo, y la interfaz solo ofrece cada acción cuando el estado la permite.

- [ ] **T-43 · Pedir la recomendación de la IA en pantalla** — RF-11
  **Hecho cuando:** al pulsar "Analizar con IA" se ve el aviso de procesando, luego los pasos numerados y los casos usados como referencia; si no hay casos aparece el aviso de recomendación general, y si la IA falla aparece el error con opción de reintentar.

## Bloque 6 — Cierre

- [ ] **T-44 · Demostrar el flujo completo** — RF-1…RF-14
  **Hecho cuando:** se recorre de punta a punta el flujo de la sección 8 de la spec (selector de supervisor → alta de técnico → registro → clasificación → asignación → técnico → filtro → detalle → iniciar → analizar → resolver → un incidente parecido recupera ese caso) y también los tres caminos de error de la IA.

- [ ] **T-45 · Comprobar a mano que Groq responde como se espera** — RNF-9
  **Hecho cuando:** existe un guion, fuera de la suite automática, que llama a Groq de verdad para clasificación y análisis, y se ha ejecutado al menos una vez confirmando que el formato de respuesta sigue siendo válido.
