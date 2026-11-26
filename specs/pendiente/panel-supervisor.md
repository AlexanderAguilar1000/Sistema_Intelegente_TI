# Pendiente — Panel del supervisor (boceto de diseño)

Estado: **incorporado a la spec 001** como RF-16 (Panel), RF-15 (indicadores en el backend) y la enmienda de paginación de RF-8, con las tareas T-46…T-55 de `task.md`. Este boceto queda como referencia visual; ante cualquier diferencia manda `spec.md`.

## Objetivo
Dar al supervisor una pantalla de **monitoreo** que le diga qué requiere su atención ahora. La pantalla actual de Incidentes (`incidente.component.html`) se mantiene como pantalla de **trabajo**: tabla, filtros y botón "Nuevo incidente".

| Vista | Propósito | Contenido |
|---|---|---|
| **Panel** (nueva, página de entrada del supervisor) | Monitorear | KPI de acción, alertas, carga por técnico, distribución |
| **Incidentes** (la actual) | Operar | Tabla, filtros, alta de incidentes, KPI de volumen |
| **Técnicos** (la actual) | Administrar | Alta y, más adelante, edición y baja lógica |

## Boceto

```
┌──────────────┬──────────────────────────────────────────────────────────────┐
│  MENÚ        │  Centro de operaciones                                       │
│              │  Panel                                    Hoy, 23 nov 2026   │
│ ▸ Panel      │                                                              │
│   Incidentes │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ │
│   Técnicos   │  │ SIN ASIGNAR│ │ ALTA PRIOR.│ │ PEND. CLASIF│ │ SIN INDEXAR│ │
│              │  │     4      │ │     2      │ │     1      │ │     1      │ │
│              │  │ ver lista ›│ │ ver lista ›│ │ ver lista ›│ │ ver lista ›│ │
│              │  └────────────┘ └────────────┘ └────────────┘ └────────────┘ │
│              │                                                              │
│              │  ┌─────────────────────────────┐ ┌─────────────────────────┐ │
│              │  │ Requiere atención           │ │ Carga por técnico       │ │
│              │  │                             │ │                         │ │
│              │  │ ● ALTA  #42 Reporte no      │ │ Marta   ███░░░░░  2     │ │
│              │  │         carga               │ │ Luis    ██████░░  5     │ │
│              │  │         Sin asignar · 5 h   │ │ Pedro   █░░░░░░░  1     │ │
│              │  │                             │ │ Sofía   ░░░░░░░░  0     │ │
│              │  │ ● ALTA  #39 VPN caída       │ │                         │ │
│              │  │         Sin iniciar · 3 h   │ │ Activos por técnico     │ │
│              │  │                             │ └─────────────────────────┘ │
│              │  │ ● MEDIA #44 Impresora       │ ┌─────────────────────────┐ │
│              │  │         Sin clasificar      │ │ Por prioridad           │ │
│              │  │                             │ │ Alta   ██░░░░░░   2     │ │
│              │  │            Ver todos ›      │ │ Media  █████░░░   5     │ │
│              │  └─────────────────────────────┘ │ Baja   ███░░░░░   3     │ │
│              │                                  └─────────────────────────┘ │
└──────────────┴──────────────────────────────────────────────────────────────┘
```

## Cómo se lee

1. **Cuatro tarjetas de acción.** Cada una cuenta algo que requiere que el supervisor haga algo y, al pulsarla, lleva al listado de Incidentes ya filtrado. Los KPI de volumen actuales (abiertos, en progreso, resueltos del mes, pendientes de clasificación) se quedan en la pantalla de Incidentes.
2. **Requiere atención** (columna izquierda). Lista corta, máximo 5 filas, ordenada por urgencia: prioridad alta primero y después mayor tiempo de espera. Cada fila muestra punto de color, número, título y motivo ("Sin asignar · 5 h"). Al pulsarla se abre el detalle. Sin alertas: "Todo al día ✓".
3. **Carga por técnico** (derecha, arriba). Barras horizontales con los incidentes activos de cada técnico, para asignar con criterio.
4. **Por prioridad** (derecha, abajo). Distribución simple; más adelante se puede añadir otra por área.

## Decisiones de diseño
- Reutilizar `app-stat-card`, los colores y los títulos con "eyebrow" ya existentes, para que parezca parte de la misma aplicación.
- Barras hechas con CSS, **sin librería de gráficos** (AGENTS.md exige confirmar antes de añadir dependencias).
- Rojo solo para prioridad alta o lo vencido; el resto en tonos neutros.
- En móvil, las dos columnas pasan a una y las tarjetas a rejilla de 2×2.
- Diseñar desde el inicio los estados de carga (esqueletos), vacío ("Todo al día") y error (con reintento).
- Menú lateral con "Panel" como primera opción del supervisor.

## Requisitos de backend
- Un endpoint de resumen, por ejemplo `GET /api/incidents/summary`, que calcule los conteos, la lista de urgentes y la carga por técnico. El frontend solo pinta lo que recibe (principio 3 y reglas de AGENTS.md).
- Solo accesible para el supervisor (403 para técnicos, como en T-06).
- Para "sin iniciar hace X horas" y los plazos por prioridad harían falta reglas de SLA nuevas en la spec.

## Puntos por comprobar antes de empezar
- Revisar cómo se calcula hoy `kpis()` en el componente de Incidentes. Si se cuenta la lista en el navegador, conviene moverlo al backend.
- Definir en la spec qué significa "sin iniciar hace X horas" (umbral por prioridad).

## Versión del técnico (pendiente de diseñar)
Mucho más simple: tres tarjetas propias (por iniciar, en progreso, resueltos) y su lista "Mis incidentes" con filtro por estado, dentro de la pantalla "Mis asignados" actual, sin vista aparte.
