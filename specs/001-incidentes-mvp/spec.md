# Spec 001 — MVP de gestión de incidentes con clasificación y diagnóstico asistidos por IA

## 1. Contexto y objetivo
Los equipos de soporte de TI resuelven incidentes que a menudo se repiten, pero las soluciones quedan dispersas y cada técnico vuelve a diagnosticar desde cero. Además, clasificar y priorizar a mano cada incidente consume tiempo del supervisor y produce criterios inconsistentes.

**Objetivo del MVP:** permitir que un supervisor registre incidentes, que un asistente de IA experto en soporte de TI los clasifique automáticamente, que el supervisor los asigne a un técnico del área responsable, y que ese técnico obtenga una recomendación de diagnóstico paso a paso basada en incidentes similares ya resueltos. Cada solución registrada se convierte en conocimiento reutilizable para futuros incidentes.

## 2. Usuarios
- **Supervisor**: registra incidentes, revisa y corrige su clasificación, los asigna o reasigna a técnicos del área responsable y da de alta técnicos. Ve todos los incidentes, en modo consulta. Los supervisores vienen precargados.
- **Técnico**: pertenece a un área técnica y solo puede recibir incidentes de esa área. Ve solo los incidentes que tiene asignados, consulta su detalle, pide una recomendación a la IA, trabaja el incidente y registra la solución. Lo da de alta un supervisor.

El MVP **no autentica a nadie**: no hay contraseñas ni sesiones. El usuario activo se elige de una lista y el sistema actúa en su nombre (RF-1).

## 3. Historias de usuario
- **HU-1** Como usuario, quiero elegir quién soy para que el sistema me muestre solo las funciones de mi rol y atribuya mis acciones a mi nombre.
- **HU-2** Como supervisor, quiero dar de alta técnicos indicando su área técnica para poder asignarles incidentes de esa área.
- **HU-3** Como supervisor, quiero registrar un incidente para que quede documentado y se clasifique automáticamente sin esfuerzo manual.
- **HU-4** Como supervisor, quiero corregir la clasificación propuesta por la IA para que tipo, prioridad y área sean correctos.
- **HU-5** Como supervisor, quiero asignar y reasignar incidentes a técnicos del área responsable para distribuir el trabajo.
- **HU-6** Como supervisor o técnico, quiero listar los incidentes y filtrarlos por prioridad para atender primero lo más urgente.
- **HU-7** Como técnico, quiero ver el detalle de un incidente asignado para entender el problema.
- **HU-8** Como técnico, quiero pulsar "Analizar con IA" para recibir pasos de solución basados en incidentes similares ya resueltos.
- **HU-9** Como técnico, quiero registrar la solución y resolver el incidente para cerrarlo y que sirva a futuros casos.
- **HU-10** Como técnico, quiero cerrar sin solución los incidentes duplicados o no reproducibles para que no ensucien el conocimiento histórico.

## 4. Requisitos funcionales

### RF-1 Identificación del usuario activo (sin autenticación)
- El sistema deberá ofrecer la lista de usuarios existentes con su nombre, rol y área, para que se elija cuál está actuando.
- Cuando se seleccione un usuario, el sistema deberá considerarlo el **usuario activo**, mostrarle solo las funciones de su rol y atribuirle todas las acciones que realice (RNF-6).
- Mientras no haya usuario activo, el sistema deberá impedir cualquier acción sobre incidentes.
- Si el usuario activo intenta una función que no corresponde a su rol, entonces el sistema deberá denegarla.
- Cuando se cambie de usuario activo, el sistema deberá aplicar de inmediato el rol y el área del nuevo usuario.
- El MVP **no verifica la identidad**: no hay contraseñas, sesiones ni control de acceso real, por lo que esta versión no es apta para producción.

### RF-2 Alta de técnicos
- Cuando el supervisor registre un técnico con nombre, usuario y **área técnica**, el sistema deberá crearlo con rol técnico y esa área.
- El sistema deberá aceptar como área únicamente un valor del catálogo de áreas de RF-5.
- Si el nombre de usuario ya existe, entonces el sistema deberá rechazar el alta e indicarlo.
- Si falta algún dato obligatorio, entonces el sistema deberá rechazar el alta indicando el campo faltante.
- Cada técnico deberá pertenecer exactamente a un área.

### RF-3 Registro de incidente y clasificación (flujo síncrono)
- Cuando el supervisor registre un incidente con título y descripción, el sistema deberá solicitar su clasificación (RF-4) y esperar la respuesta hasta el límite de RNF-1 antes de confirmar el registro.
- Cuando la clasificación llegue a tiempo y sea válida, el sistema deberá guardar el incidente en estado **Registrado** ya clasificado, con fecha de creación y el supervisor que lo creó, y mostrárselo clasificado.
- Si se supera el límite de espera o la clasificación no es válida, entonces el sistema deberá guardar igualmente el incidente en estado **Registrado**, marcarlo como **pendiente de clasificación** e informar al supervisor en la misma pantalla.
- Si falta el título o la descripción, entonces el sistema deberá rechazar el registro indicando el campo faltante, sin consultar a la IA.
- El sistema no deberá perder nunca un incidente por un fallo de la IA.

### RF-4 Clasificación automática con IA
- Cuando se solicite la clasificación de un incidente, el sistema deberá pedir al asistente de IA, en el papel de experto en soporte de TI, que determine su **tipo**, **prioridad** y **área** a partir del título y la descripción.
- El sistema deberá aceptar únicamente respuestas cuyos tres valores pertenezcan a los catálogos de RF-5; si alguno no pertenece, deberá descartar la respuesta completa.
- Cuando la clasificación sea válida, el sistema deberá guardarla junto al incidente y marcarla como "clasificado por IA".
- Cuando el supervisor pida reintentar la clasificación de un incidente pendiente, el sistema deberá volver a solicitarla con las mismas reglas.
- La marca de pendiente de clasificación deberá ser independiente del estado del incidente.

### RF-5 Catálogos cerrados
- El sistema deberá usar como **prioridades** exclusivamente: Alta, Media, Baja.
- El sistema deberá usar como **tipos** exclusivamente: Bug, Rendimiento, Acceso, Hardware, Red, Configuración.
- El sistema deberá usar como **áreas técnicas** exclusivamente: Infraestructura, Aplicaciones, Base de datos, Seguridad, Soporte a usuario.
- El área representa al equipo técnico responsable de atender el incidente, y es la que determina qué técnicos pueden recibirlo (RF-7).

### RF-6 Corrección manual de la clasificación
- Mientras un incidente no esté cerrado (Resuelto o Cerrado sin solución), cuando el supervisor modifique su tipo, prioridad o área eligiendo valores del catálogo, el sistema deberá guardar el cambio y marcar la clasificación como "corregida manualmente".
- Cuando el supervisor clasifique a mano un incidente pendiente de clasificación, el sistema deberá quitarle esa marca.
- Si el cambio de área deja al técnico asignado fuera del área responsable, entonces el sistema deberá retirarle la asignación y devolver el incidente a estado **Registrado**, informando al supervisor.
- Si el incidente está cerrado, entonces el sistema deberá impedir cambiar su clasificación.

### RF-7 Asignación y reasignación
- Si un incidente está pendiente de clasificación, entonces el sistema deberá impedir asignarlo hasta que tenga área (por reintento de IA o clasificación manual).
- Cuando el supervisor asigne un incidente clasificado a un técnico del área responsable, el sistema deberá pasarlo a **Asignado** y registrar técnico, fecha y supervisor.
- Si el técnico elegido no pertenece al área del incidente, entonces el sistema deberá rechazar la asignación indicando el motivo.
- Cuando el supervisor seleccione un incidente para asignar, el sistema deberá ofrecer únicamente técnicos del área responsable.
- Mientras un incidente esté Asignado o En progreso, cuando el supervisor lo reasigne a otro técnico del área, el sistema deberá cambiar el responsable, dejarlo en estado **Asignado** y registrar la reasignación.
- Si el incidente está cerrado, entonces el sistema deberá impedir asignarlo o reasignarlo.
- Si un técnico que ha dejado de ser el responsable intenta actuar sobre el incidente, entonces el sistema deberá rechazar la acción explicando que ya no está asignado a él.

### RF-8 Listado y filtrado
- Cuando el supervisor abra el listado, el sistema deberá mostrar todos los incidentes con título, estado, tipo, prioridad, área, técnico asignado y fecha.
- Cuando el técnico abra el listado, el sistema deberá mostrar solo los incidentes que tiene asignados.
- Cuando el usuario filtre por prioridad (Alta, Media o Baja), el sistema deberá mostrar solo los incidentes de esa prioridad dentro de los que puede ver.
- Donde un incidente esté pendiente de clasificación, el sistema deberá mostrarlo sin tipo, prioridad ni área, y excluirlo de los filtros por prioridad.
- El sistema deberá permitir al supervisor localizar los incidentes pendientes de clasificación, ya que son los únicos que no puede asignar.
- Si no hay incidentes que mostrar, entonces el sistema deberá indicar que la lista está vacía.

### RF-9 Detalle del incidente
- Cuando el técnico abra un incidente asignado a él, el sistema deberá mostrar título, descripción, clasificación, estado, fechas, la última recomendación mostrada mientras la pantalla siga abierta si la hubo, y la solución o el motivo de cierre si existen.
- Cuando el supervisor abra cualquier incidente, el sistema deberá mostrar la misma información en modo consulta, sin la opción "Analizar con IA".
- Si un técnico intenta abrir un incidente que no tiene asignado, entonces el sistema deberá denegar el acceso.

### RF-10 Inicio del trabajo
- Cuando el técnico indique que empieza a trabajar un incidente Asignado a él, el sistema deberá pasarlo a **En progreso**.

### RF-11 Analizar con IA (recomendación basada en casos similares)
- Mientras un incidente esté Asignado o En progreso, el sistema deberá ofrecer la opción "Analizar con IA" únicamente al técnico asignado.
- Cuando el técnico pulse "Analizar con IA", el sistema deberá buscar en el histórico (RF-13) los **3 incidentes resueltos más parecidos en significado**, sin limitarse a su área, descartando los que no superen el umbral mínimo de parecido.
- Cuando existan casos por encima del umbral, el sistema deberá enviar a la IA el incidente actual y las soluciones de esos casos, y pedirle una recomendación de diagnóstico paso a paso basada en ellos.
- Cuando la IA devuelva la recomendación, el sistema deberá mostrar los pasos numerados y los incidentes similares usados como referencia.
- Si ningún caso supera el umbral o el histórico está vacío, entonces el sistema deberá pedir una recomendación general y mostrarla indicando claramente que no se basa en casos previos.
- El sistema deberá validar que la respuesta contenga al menos un paso con texto; si no lo cumple, deberá tratarla como fallo.
- Si la IA no responde, falla o devuelve una respuesta inválida, entonces el sistema deberá mostrar un mensaje de error, permitir reintentar y no alterar el incidente.
- Mientras un análisis esté en curso, el sistema deberá indicar que se está procesando e impedir lanzar otro análisis del mismo incidente.
- El sistema deberá presentar la recomendación como sugerencia; la decisión final es del técnico.

### RF-12 Resolución y registro de la solución
- Cuando el técnico registre una solución de al menos 30 caracteres en un incidente En progreso asignado a él, el sistema deberá pasarlo a **Resuelto** y guardar la solución, la fecha de resolución y el técnico.
- Si la solución está vacía o no alcanza la longitud mínima, entonces el sistema deberá rechazar la resolución indicando el motivo.
- Si el incidente no está En progreso, entonces el sistema deberá impedir resolverlo.
- Mientras un incidente esté Resuelto, el sistema deberá impedir modificar su contenido, su clasificación y su asignación, y deberá impedir reabrirlo; solo se permitirá completar su indexación pendiente (RF-13).

### RF-13 Histórico y búsqueda de casos similares
- El histórico deberá estar formado por los propios incidentes del sistema en estado Resuelto junto con su solución; no existirá un almacén de conocimiento separado ni carga inicial de casos.
- Cuando un incidente pase a Resuelto, el sistema deberá indexarlo por significado junto con su solución para que pueda recuperarse en futuros análisis (RF-11).
- Si la indexación falla, entonces el sistema deberá mantener el incidente como Resuelto, marcarlo como **sin indexar** y permitir al supervisor lanzar la indexación de nuevo.
- Mientras un incidente resuelto esté sin indexar, el sistema no deberá recuperarlo como caso similar.
- El sistema no deberá incorporar al histórico los incidentes cerrados sin solución (RF-14).

### RF-14 Cierre sin solución
- Cuando el técnico cierre un incidente Asignado o En progreso asignado a él indicando un motivo (duplicado, no reproducible o descartado), el sistema deberá pasarlo a **Cerrado sin solución** y registrar motivo, fecha y técnico.
- Si no se indica motivo, entonces el sistema deberá rechazar el cierre.
- El sistema no deberá exigir texto de solución en este cierre.
- Mientras un incidente esté Cerrado sin solución, el sistema deberá impedir modificarlo, reasignarlo o reabrirlo.

## 5. Requisitos no funcionales
- **RNF-1 Tiempo de clasificación:** el registro de un incidente no deberá esperar la clasificación más de **10 segundos**; superado ese tiempo, el incidente queda pendiente de clasificación (RF-3).
- **RNF-2 Tiempo de análisis:** la recomendación deberá mostrarse en un máximo de **30 segundos**; superado ese tiempo se trata como fallo (RF-11).
- **RNF-3 Secretos:** las claves de acceso a la IA y a la base de datos nunca deberán aparecer en el código, en el repositorio ni en la interfaz.
- **RNF-4 Privacidad:** a la IA solo deberá enviarse el contenido del incidente y de los casos similares, nunca datos de cuentas de usuario. La interfaz deberá advertir de no escribir contraseñas en descripciones ni soluciones.
- **RNF-5 Texto de usuario como dato:** el contenido escrito por usuarios y las soluciones históricas deberán enviarse a la IA claramente delimitados como datos, indicando que no debe obedecer instrucciones contenidas en ellos.
- **RNF-6 Trazabilidad:** deberá quedar registrado quién y cuándo crea, clasifica o corrige, asigna, reasigna, inicia, resuelve o cierra cada incidente.
- **RNF-7 Idioma:** la interfaz, los mensajes y las respuestas de la IA deberán estar en español.
- **RNF-8 Resiliencia:** un fallo de la IA nunca deberá provocar la pérdida de un incidente ni de una solución ya registrada.
- **RNF-9 Verificabilidad:** las pruebas automatizadas deberán ejecutarse con respuestas de IA simuladas y cubrir las reglas del sistema (validación de catálogo, superación del límite de espera, ausencia de casos similares, respuesta de análisis inválida, un solo análisis simultáneo). La calidad real de las respuestas de la IA se verifica manualmente en la demostración.

## 6. Casos límite
- La IA devuelve prioridad válida pero tipo o área fuera de catálogo → se descarta la respuesta completa y el incidente queda pendiente de clasificación.
- Incidente pendiente de clasificación → no se puede asignar hasta que tenga área.
- El área responsable no tiene ningún técnico dado de alta → el supervisor no puede asignar y el sistema debe indicarlo al intentarlo.
- El supervisor cambia el área de un incidente ya asignado → se retira la asignación y vuelve a Registrado.
- El supervisor reasigna mientras el técnico anterior trabaja → manda el supervisor; lo que envíe el técnico anterior después se rechaza.
- Dos supervisores asignan el mismo incidente a la vez → prevalece una sola asignación y ambas quedan trazadas.
- Histórico vacío (primer uso) → "Analizar con IA" da recomendación general con aviso.
- Casos similares por debajo del umbral → se descartan y se trata como si no hubiera similares.
- El propio incidente nunca puede recuperarse como caso similar de sí mismo.
- Descripción muy corta o ambigua ("no funciona") → la IA puede clasificarla; si no puede, queda pendiente de clasificación.
- La IA responde al análisis con pasos vacíos o incoherentes → se trata como fallo y el técnico puede reintentar.
- El técnico pulsa "Analizar con IA" varias veces seguidas → solo se procesa un análisis a la vez.
- Se intenta resolver un incidente Asignado sin haberlo iniciado → se rechaza (debe pasar por En progreso).
- Un incidente resuelto queda sin indexar por un fallo de la IA → no se recupera como caso similar hasta que el supervisor relance la indexación.

## 7. Fuera de alcance (MVP)
- Diseño del modelo de datos (se definirá en una fase posterior).
- Reapertura de incidentes cerrados, con o sin solución.
- Carga inicial de incidentes históricos: el histórico arranca vacío.
- Autenticación, contraseñas y gestión de sesiones: el usuario activo se elige de una lista y el sistema confía en esa elección.
- Registro abierto de usuarios, edición o baja de usuarios.
- Técnicos que pertenezcan a más de un área.
- Creación de incidentes por usuarios finales o por técnicos.
- Edición o eliminación del título y la descripción de un incidente una vez registrado.
- Guardar las recomendaciones de la IA para consultarlas después de la sesión.
- Notificaciones (correo, avisos en tiempo real), SLA y tiempos de respuesta comprometidos.
- Procesos automáticos en segundo plano: los reintentos de clasificación y de indexación los lanza una persona.
- Detección automática de incidentes duplicados.
- Adjuntos, comentarios o historial de conversación dentro del incidente.
- Informes, métricas o paneles.

## 8. Criterios de finalización
- Todos los criterios de aceptación de RF-1 a RF-14 se cumplen y tienen pruebas automatizadas que pasan, con la IA simulada según RNF-9 (principio 4 de la constitución).
- Se demuestra el flujo completo: se selecciona un supervisor → crea un técnico de un área → registra un incidente → queda clasificado → lo asigna a un técnico de esa área → se cambia al usuario técnico, que ve solo sus incidentes y filtra por prioridad → abre el detalle → lo inicia → pulsa "Analizar con IA" → registra la solución → un nuevo incidente parecido recupera ese caso como referencia.
- Se demuestran los caminos de error de la IA: clasificación fuera de plazo o inválida (pendiente + reintento + clasificación manual), análisis fallido o inválido (mensaje + reintento) e indexación fallida (resuelto sin indexar + reindexación).
- Se demuestra que un incidente no puede asignarse a un técnico de otra área ni sin clasificar.
- Se demuestra que un técnico no puede ver ni actuar sobre incidentes de otro técnico.
- No quedan dudas abiertas que bloqueen ningún requisito: las pendientes de la sección 9 son de detalle y no condicionan el diseño.

## 9. Dudas abiertas (menores, no bloqueantes)
- [NECESITA ACLARACIÓN] Orden por defecto y paginación del listado, y filtros adicionales por estado, tipo o área.
- [NECESITA ACLARACIÓN] Qué fechas se muestran en el detalle (creación, asignación, inicio, cierre).
- [NECESITA ACLARACIÓN] Si el técnico sigue viendo en su listado los incidentes que ya cerró.
- [NECESITA ACLARACIÓN] Si las marcas "clasificado por IA" y "corregida manualmente" son visibles para el usuario o solo internas.
- [NECESITA ACLARACIÓN] Si la traza de acciones (RNF-6) se muestra en la interfaz o solo se registra.
- [NECESITA ACLARACIÓN] Longitud máxima de título, descripción y solución.
- [NECESITA ACLARACIÓN] Valor concreto del umbral mínimo de parecido (RF-11), a calibrar con datos reales.
- [NECESITA ACLARACIÓN] Si un supervisor puede corregir o reasignar incidentes creados por otro supervisor.
- [NECESITA ACLARACIÓN] Cuántos supervisores y técnicos se precargan para la demostración.
