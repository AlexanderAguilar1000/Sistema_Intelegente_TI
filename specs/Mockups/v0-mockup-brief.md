# Brief para maquetado en v0 — Sistema de Incidentes de TI (MVP)

> Este documento resume, solo desde el punto de vista de interfaz, lo necesario para generar un maquetado (mockup) en v0. No incluye endpoints, contratos ni decisiones de arquitectura backend: son irrelevantes para el diseño visual. Idioma de toda la interfaz: **español**.

## 1. Qué es el sistema

Sistema interno de soporte de TI. Un supervisor registra incidentes, una IA los clasifica automáticamente (tipo, prioridad, área), el supervisor los asigna a un técnico del área responsable, y ese técnico ve una recomendación de diagnóstico paso a paso (basada en incidentes parecidos ya resueltos) antes de resolver el incidente. Cada solución resuelta queda como conocimiento reutilizable.

No es una app pública ni de consumo: es una herramienta de trabajo interna, tipo panel de soporte/ticketing (piensa en algo como Jira Service Management o Zendesk, pero mucho más simple).

## 2. No hay login real

No existe autenticación. En vez de login, hay un **selector de usuario activo**: una lista de usuarios (nombre, rol, área) de la que se elige "quién soy". Esto debería representarse como un componente visible siempre (por ejemplo, un selector en la barra superior) que muestra el usuario activo actual y permite cambiarlo. Mientras no haya usuario elegido, no se puede hacer nada con incidentes.

## 3. Roles y qué ve cada uno

### Supervisor
- Ve **todos** los incidentes, en modo consulta (no puede actuar como técnico).
- Da de alta técnicos (nombre, usuario, área técnica).
- Registra incidentes nuevos.
- Corrige la clasificación (tipo/prioridad/área) de un incidente no cerrado.
- Reintenta la clasificación por IA si quedó pendiente.
- Asigna y reasigna incidentes a técnicos del área responsable.
- Puede relanzar la indexación de un incidente resuelto que quedó "sin indexar".
- **No tiene** el botón "Analizar con IA" (eso es solo del técnico).

### Técnico
- Pertenece a **una sola** área técnica (Infraestructura, Aplicaciones, Base de datos, Seguridad, Soporte a usuario).
- Ve **solo** los incidentes asignados a él.
- Abre el detalle de un incidente asignado, lo inicia, pide análisis de IA, registra la solución o lo cierra sin solución.
- No puede ver ni actuar sobre incidentes de otros técnicos.

## 4. Catálogos cerrados (usar exactamente estos valores, en español, como listas fijas — no editables por el usuario)

- **Prioridad:** Alta, Media, Baja.
- **Tipo:** Bug, Rendimiento, Acceso, Hardware, Red, Configuración.
- **Área técnica:** Infraestructura, Aplicaciones, Base de datos, Seguridad, Soporte a usuario.
- **Estado del incidente:** Registrado, Asignado, En progreso, Resuelto, Cerrado sin solución.
- **Motivo de cierre sin solución:** Duplicado, No reproducible, Descartado.

Sugerencia visual: usar chips/badges de color para prioridad (ej. Alta=rojo, Media=ámbar, Baja=verde) y para estado (colores distintos por estado). El tipo y el área pueden ser badges neutros o con icono.

## 5. Entidades y campos a mostrar

### Usuario
- Nombre completo, nombre de usuario, rol (Supervisor/Técnico), área (solo técnicos).

### Incidente
- Título, descripción (texto largo).
- Estado (ver catálogo).
- Clasificación: tipo, prioridad, área — o "pendiente de clasificación" si la IA no pudo clasificarlo aún (sin tipo/prioridad/área).
- Marca de origen de la clasificación: "clasificado por IA" o "corregida manualmente" (puede mostrarse como detalle secundario, no es el foco).
- Creado por (supervisor) y fecha de creación.
- Técnico asignado y fecha de asignación (si aplica).
- Fecha de inicio (si fue iniciado).
- Solución (texto largo) y fecha de resolución (si fue resuelto), o motivo de cierre y fecha (si fue cerrado sin solución).
- Indicador "sin indexar" cuando un incidente resuelto no pudo indexarse (solo relevante para supervisor).

### Recomendación de IA (pantalla de análisis, solo técnico)
- Lista numerada de pasos de diagnóstico.
- Lista de incidentes de referencia usados (título + porcentaje/score de parecido), o aviso de que es una recomendación general sin casos previos si no hay histórico parecido.
- Estado de carga mientras se procesa (puede tardar hasta ~30s) y estado de error con opción de reintentar.

## 6. Pantallas necesarias

1. **Selector de usuario activo** — lista de usuarios con nombre, rol y área; al elegir uno, queda como activo (visible en la barra superior en todas las pantallas siguientes).
2. **Alta de técnico** (solo supervisor) — formulario: nombre completo, nombre de usuario, área técnica (select del catálogo). Debe poder mostrar error si el usuario ya existe o falta un campo.
3. **Listado de incidentes** (RF-8):
   - Supervisor: todos los incidentes, columnas: título, estado, tipo, prioridad, área, técnico asignado, fecha.
   - Técnico: solo los suyos, mismas columnas salvo que el técnico asignado es obvio (puede omitirse esa columna).
   - Filtro por prioridad (Alta/Media/Baja).
   - Los incidentes "pendientes de clasificación" se muestran sin tipo/prioridad/área y quedan fuera del filtro por prioridad; el supervisor necesita poder ubicarlos fácilmente (ej. filtro o badge "Pendiente de clasificación").
   - Mensaje de "lista vacía" cuando no hay incidentes que mostrar.
4. **Registro de incidente** (solo supervisor, RF-3):
   - Formulario: título, descripción.
   - Aviso visible de no escribir contraseñas ni datos sensibles en la descripción.
   - Al guardar, muestra el resultado de la clasificación automática (tipo/prioridad/área) o el aviso de "pendiente de clasificación" si la IA no respondió a tiempo o la respuesta no fue válida.
5. **Detalle de incidente**:
   - Común a ambos roles: título, descripción, clasificación, estado, fechas relevantes, solución o motivo de cierre si existen.
   - Supervisor: modo consulta, con acciones de corregir clasificación, reintentar clasificación (si está pendiente), asignar/reasignar, reindexar (si está "sin indexar"). Sin botón de análisis IA.
   - Técnico (solo si el incidente es suyo): botón "Iniciar" (si está Asignado), botón "Analizar con IA" (si está Asignado o En progreso), formulario de solución para resolver (mínimo 30 caracteres, si está En progreso), y opción de "Cerrar sin solución" con selector de motivo (si está Asignado o En progreso).
6. **Corrección de clasificación** (modal o sección dentro del detalle, solo supervisor) — selects de tipo, prioridad y área con los valores actuales precargados.
7. **Asignación / reasignación** (modal o sección dentro del detalle, solo supervisor) — select de técnico, filtrado para mostrar solo técnicos del área responsable del incidente; si no hay técnicos en esa área, mostrarlo claramente.
8. **Análisis con IA** (dentro del detalle del técnico) — estado de carga, resultado con pasos numerados + incidentes de referencia, o estado de error con botón de reintentar.
9. **Resolución del incidente** (formulario dentro del detalle del técnico) — textarea para la solución con contador de caracteres (mínimo 30) y validación visible.
10. **Cierre sin solución** (formulario/modal dentro del detalle del técnico) — select de motivo (Duplicado, No reproducible, Descartado), obligatorio.

## 7. Reglas de UI relevantes para el mockup (qué mostrar/ocultar u ocultar según estado y rol)

- Sin usuario activo elegido: bloquear cualquier acción sobre incidentes (se puede representar como pantalla inicial forzando el selector).
- "Analizar con IA" solo aparece para el técnico asignado, y solo si el incidente está Asignado o En progreso.
- Mientras un análisis está en curso, deshabilitar el botón y mostrar que se está procesando (no se puede lanzar dos veces).
- Un incidente Resuelto o Cerrado sin solución es de solo lectura: no se pueden editar, reasignar ni reabrir.
- Un incidente "pendiente de clasificación" no se puede asignar hasta que tenga área (por reintento de IA o corrección manual).
- Los mensajes de error y validación deben estar en español y ser claros (ej. "Falta el título", "La solución debe tener al menos 30 caracteres", "Este técnico no pertenece al área del incidente").

## 8. Tono visual sugerido

Panel de trabajo interno, denso en información pero legible: tipografía clara, buen uso de badges de color para estado/prioridad, tablas para listados, formularios simples de una columna. No es una landing ni un producto de marketing — prioriza claridad y velocidad de lectura para técnicos y supervisores que lo usan varias veces al día.

## 9. Fuera de alcance para el mockup

No incluir: pantallas de login/password, notificaciones en tiempo real, paneles de métricas/reportes, adjuntos o comentarios dentro del incidente, ni reapertura de incidentes cerrados. El histórico de incidentes resueltos no tiene pantalla propia: solo se usa internamente para alimentar la recomendación de IA.
