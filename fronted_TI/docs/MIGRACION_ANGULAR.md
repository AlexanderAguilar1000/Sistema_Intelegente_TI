# Guía de migración: Mockup V0 (Next.js/React) → Angular

Documento de referencia para reconstruir en Angular el prototipo **Gestión de Incidentes TI**. Cubre funcionalidad, modelo de datos, reglas de negocio, estructura de pantallas, diseño visual y un mapa de equivalencias React → Angular.

> El mockup es 100 % frontend: no hay backend, API ni autenticación real. Todos los datos son mock en memoria. Ver la sección 12 para lo que falta por definir.

> **Migración vista por vista:** la sección **13. Vistas por rol** contiene una ficha completa de cada pantalla (supervisor y técnico) con sus componentes, datos, reglas, textos y criterios de aceptación, más el catálogo de componentes reutilizables (13.2). Es el punto de entrada para pasarla a Angular junto con las capturas del diseño.

---

## 1. Resumen del prototipo

- **Nombre:** Gestión de Incidentes TI — "Centro de operaciones de soporte".
- **Idioma de la UI:** español (`lang="es"`). Formato de fecha `dd/MM/yyyy HH:mm`.
- **Propósito:** registrar, clasificar (con IA), asignar y resolver incidentes de soporte de TI.
- **Roles:** `Supervisor` y `Técnico`. No hay login: un selector de "usuario activo" permite simular ambos roles.
- **Arquitectura original:** una sola página (`app/page.tsx`), sin rutas. La navegación se controla con estado (`activePage`, `selectedIncident`). En Angular conviene pasar a **rutas reales**.

### Stack original
| Elemento | Uso |
|---|---|
| Next.js 16 + React 19 | Framework |
| Tailwind CSS 4 + `tw-animate-css` | Estilos utilitarios |
| shadcn/ui sobre `@base-ui/react` | Componentes (Button, Badge, Card, Input, Textarea, Avatar, Separator, Select, Dialog) |
| `class-variance-authority`, `clsx`, `tailwind-merge` | Variantes de clases |
| `lucide-react` | Iconos |
| `@vercel/analytics` | Analítica (solo en producción; se puede omitir) |

Componentes shadcn generados pero **no usados** en la página: `alert`, `table`, `tabs`.

---

## 1.1 Roles: qué hace cada uno y a qué vistas accede

> Fuente: especificación funcional del proyecto (`decision.md` / spec RF-*). Donde el mockup difiere de la especificación se indica con ⚠️. **Ante conflicto, manda esta sección para reglas de negocio y las capturas para lo visual.**

### A. Acciones por rol

| Acción | Supervisor | Técnico | Notas |
|---|:---:|:---:|---|
| Dar de alta técnicos (nombre, usuario, área técnica) | ✅ | ❌ | |
| Registrar incidentes nuevos (título, descripción) | ✅ | ❌ | La IA clasifica al registrar |
| Ver listado global de incidentes (cualquier técnico/área) | ✅ | ❌ | |
| Ver listado de incidentes propios | ❌ | ✅ | Solo los asignados a él (de su área) |
| Ver detalle de un incidente | ✅ cualquiera (modo consulta) | ✅ solo si es suyo | |
| Filtrar listado por prioridad | ✅ | ✅ | ⚠️ El mockup añade filtro por estado, filtro por prioridad  y búsqueda al supervisor; el técnico solo búsqueda + prioridad |
| Localizar incidentes pendientes de clasificación | ✅ | ❌ | Badge "Pendiente" en la tabla |
| Corregir clasificación (tipo/prioridad/área) | ✅ solo si NO está cerrado | ❌ | Deja `classificationSource = 'Corregida manualmente'` |
| Reintentar clasificación por IA (si quedó pendiente) | ✅ solo si `pendingClassification` y el incidente no está cerrado | ❌ | Botón **"Reintentar clasificación IA"** ya implementado en el mockup (ver 7.10 y 7.15) |
| Asignar incidente clasificado | ✅ | ❌ | Solo a técnicos del **área responsable**; no si está pendiente de clasificación (sin área) |
| Reasignar (cambiar técnico) | ✅ solo si el incidente está **Asignado** o **En progreso** | ❌ | **Decisión confirmada** (resuelve la inconsistencia entre `decision.md` y RF-7): el nuevo técnico debe pertenecer al **área responsable** del incidente. En *Registrado* no se reasigna, se asigna por primera vez. El mockup lo permite en cualquier estado no final; hay que restringirlo |
| Relanzar indexación de incidente Resuelto "sin indexar" | ✅ | ❌ | |
| Iniciar incidente (Asignado → En progreso) | ❌ | ✅ solo el asignado | |
| "Analizar con IA" | ❌ | ✅ solo el asignado, con incidente Asignado o En progreso | |
| Resolver (registrar solución) | ❌ | ✅ solo si está **En progreso** | Solución mínimo 30 caracteres |
| Cerrar sin solución | ❌ | ✅ si está Asignado o En progreso | Motivo: Duplicado / No reproducible / Descartado (técnicos: `DUPLICATE`, `NOT_REPRODUCIBLE`, `DISCARDED`) |
| Cambiar el estado del incidente | ❌ (lo hace el sistema) | ✅ único rol que lo cambia | Mediante iniciar, resolver, cerrar sin solución |
| Reasignar/editar incidente ya cerrado (Resuelto / Cerrado sin solución) | ❌ | ❌ | Nadie puede reabrir |

**Restricciones adicionales**
- **Supervisor no puede:** usar "Analizar con IA", cambiar el estado directamente, resolver ni cerrar incidentes, editar/reasignar cerrados, asignar un incidente sin área, ni asignar a un técnico de otra área.
- **Técnico no puede:** ver o actuar sobre incidentes de otros técnicos o áreas, registrar incidentes, dar de alta técnicos, corregir clasificación, asignar/reasignar, reindexar, resolver un incidente que no esté En progreso (debe iniciarlo antes), actuar tras haber sido **reasignado** por el supervisor (pierde acceso), ni reabrir cerrados.

**Transiciones automáticas de estado** (el sistema las aplica según la acción):
```
Registrado ─(supervisor asigna)→ Asignado ─(técnico inicia)→ En progreso ─(técnico resuelve)→ Resuelto
                                    └──────────(técnico cierra sin solución)──────────────────→ Cerrado sin solución
```

### B. Vistas a las que accede cada rol

| # | Vista / componente | Ruta Angular | Supervisor | Técnico | Detalle |
|---|---|---|:---:|:---:|---|
| 1 | Selector de usuario activo | modal global | ✅ | ✅ | Simula el login; permite cambiar de rol |
| 2 | Listado global de incidentes | `/incidentes` | ✅ | ❌ | Columnas: título, estado, tipo, prioridad, área, técnico asignado, fecha. Filtros: prioridad (+ estado y búsqueda en el mockup) |
| 3 | Listado de incidentes propios | `/mis-asignados` | ❌ | ✅ | Solo los suyos; filtro por prioridad |
| 4 | Registro de incidente (formulario) | `/incidentes/nuevo` | ✅ | ❌ | Título y descripción |
| 5 | Alta de técnico (formulario/modal) | `/administracion` (+ diálogo) | ✅ | ❌ | Nombre, usuario, área técnica |
| 6 | Detalle de incidente | `/incidentes/:id` | ✅ cualquiera, modo consulta (sin botón IA) | ✅ solo si es suyo | Las acciones visibles dependen del rol y del estado |
| 7 | Modal de corrección de clasificación | diálogo en el detalle | ✅ (incidente no cerrado) | ❌ | Tipo, prioridad, área |
| 8 | Acción "Reintentar clasificación por IA" | botón en el detalle | ✅ (si está pendiente) | ❌ | Implementado en el mockup (ver 7.15) |
| 9 | Modal de asignación / reasignación | diálogo en el detalle | ✅ | ❌ | Solo técnicos del área responsable |
| 10 | Acción "Relanzar indexación" | botón en el detalle | ✅ (Resuelto y sin indexar) | ❌ | |
| 11 | Pantalla / panel de Análisis con IA | panel en el detalle | ❌ | ✅ (solo el asignado) | Pasos numerados + casos similares. Al pulsar un caso similar se despliega su detalle **dentro del mismo panel**, sin cambiar de pestaña ni de ruta (ver 7.16) |
| 12 | Formulario de resolución | sección en el detalle | ❌ | ✅ (solo En progreso) | Solución ≥ 30 caracteres |
| 13 | Modal de cierre sin solución | diálogo en el detalle | ❌ | ✅ (Asignado o En progreso) | Selector de motivo |

Resumen de acceso a rutas (para los guards):

| Ruta | Supervisor | Técnico | Guard |
|---|:---:|:---:|---|
| `/incidentes` | ✅ | ❌ (redirige a `/mis-asignados`) | `roleGuard('Supervisor')` |
| `/mis-asignados` | ❌ (redirige a `/incidentes`) | ✅ | `roleGuard('Técnico')` |
| `/incidentes/nuevo` | ✅ | ❌ | `roleGuard('Supervisor')` |
| `/administracion` | ✅ | ❌ | `roleGuard('Supervisor')` |
| `/incidentes/:id` | ✅ | ✅ solo si `assignedTo === username` | Guard por asignación |

### C. Visibilidad de botones en el detalle (matriz rol × estado)

| Botón | Registrado | Asignado | En progreso | Resuelto | Cerrado sin solución |
|---|---|---|---|---|---|
| Corregir clasificación (Sup.) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Reintentar clasificación IA (Sup.) | ✅ si está pendiente | — | — | — | — |
| Asignar / Reasignar (Sup.) | ✅ asignar (si tiene área) | ✅ reasignar | ✅ reasignar | ❌ | ❌ |
| Relanzar indexación (Sup.) | ❌ | ❌ | ❌ | ✅ si `unindexed` | ❌ |
| Iniciar trabajo (Téc.) | ❌ | ✅ | ❌ (ya iniciado) | ❌ | ❌ |
| Analizar con IA (Téc.) | ❌ | ✅ | ✅ | ❌ | ❌ |
| Resolver (Téc.) | ❌ | ❌ | ✅ | ❌ | ❌ |
| Cerrar sin solución (Téc.) | ❌ | ✅ | ✅ | ❌ | ❌ |

Nota: en un incidente **Registrado** no hay técnico asignado, por lo que un técnico nunca lo ve (solo ve los asignados a él).

---

## 2. Stack recomendado en Angular

| React / V0 | Angular sugerido |
|---|---|
| Next.js App Router | Angular 18+ standalone components + `provideRouter` |
| `useState` | `signal()` / `computed()` |
| `useMemo` | `computed()` |
| Props / callbacks | `input()` / `output()` |
| Contexto de estado global (aquí `page.tsx`) | Servicios `@Injectable({providedIn:'root'})` con signals |
| Tailwind 4 | Se puede mantener (funciona con Angular) o migrar a SCSS |
| shadcn/base-ui | **Angular Material** (`MatDialog`, `MatSelect`, `MatFormField`, `MatButton`) o **Spartan/ui** (port de shadcn para Angular) o **PrimeNG**. Recomendado: Spartan/ui si se quiere fidelidad visual; Angular Material si se prioriza mantenibilidad |
| `lucide-react` | `lucide-angular` (mismos nombres de icono) |
| `cva` variantes | Clases CSS condicionales con `[class]` / `[ngClass]` |
| `Dialog` | `MatDialog` o `<dialog>` con CDK Overlay |
| `Select` | `MatSelect` o `<select>` nativo estilizado |
| Formularios | `ReactiveFormsModule` (`FormBuilder`, `Validators`) |

Comando base:
```bash
ng new incidentes-ti --standalone --routing --style=scss
npm i lucide-angular
# opcional: npm i tailwindcss @tailwindcss/postcss
# opcional: ng add @angular/material
```

---

## 3. Modelo de datos (TypeScript, se copia tal cual)

Archivo original: `types/index.ts`. Sirve directamente como `src/app/models/incident.models.ts`.

```ts
export type Priority = 'Alta' | 'Media' | 'Baja';
export type IncidentType = 'Bug' | 'Rendimiento' | 'Acceso' | 'Hardware' | 'Red' | 'Configuración';
export type TechArea = 'Infraestructura' | 'Aplicaciones' | 'Base de datos' | 'Seguridad' | 'Soporte a usuario';
export type IncidentStatus = 'Registrado' | 'Asignado' | 'En progreso' | 'Resuelto' | 'Cerrado sin solución';
export type ClosureReason = 'Duplicado' | 'No reproducible' | 'Descartado';
export type UserRole = 'Supervisor' | 'Técnico';
export type ClassificationSource = 'Clasificado por IA' | 'Corregida manualmente';

export interface User {
  id: string;
  fullName: string;
  username: string;
  role: UserRole;
  area?: TechArea;            // solo técnicos
}

export interface Incident {
  id: string;                 // formato INC-AAAA-NNNN
  title: string;
  description: string;
  status: IncidentStatus;
  type?: IncidentType;
  priority?: Priority;
  area?: TechArea;
  classificationSource?: ClassificationSource;
  createdBy: string;          // username del supervisor
  createdAt: string;          // 'dd/MM/yyyy HH:mm' (en Angular: usar Date/ISO)
  assignedTo?: string;        // username del técnico
  assignedAt?: string;
  startedAt?: string;
  solution?: string;
  resolvedAt?: string;
  closureReason?: ClosureReason;
  closedAt?: string;
  pendingClassification?: boolean;  // sin type/priority/area todavía
  unindexed?: boolean;              // resuelto pero no indexado en la base de conocimiento de la IA
}

// Caso similar: incidente ya resuelto que la IA usa como referencia (con detalle completo)
export interface SimilarCase {
  id: string;
  title: string;
  similarity: number;         // porcentaje 0-100
  type: IncidentType;
  priority: Priority;
  area: TechArea;
  status: IncidentStatus;     // en la práctica siempre 'Resuelto'
  description: string;
  solution: string;           // solución aplicada
  resolvedBy: string;         // nombre completo del técnico
  resolvedAt: string;
}

export interface AIRecommendation {
  steps: string[];
  referenceIncidents: SimilarCase[];
  loading?: boolean;
  error?: string | null;
}
```

Recomendación: guardar fechas como `Date` o ISO string y formatear con `DatePipe` (`'dd/MM/yyyy HH:mm'`). El mockup usa strings ya formateados, por lo que `createdAt.split(' ')[0]` se reemplaza por `| date:'dd/MM/yyyy'`.

---

## 4. Datos mock (semilla)

### Usuarios
| id | fullName | username | role | area |
|---|---|---|---|---|
| u1 | Ana García | ana.garcia | Supervisor | — |
| u2 | Carlos Ruiz | carlos.ruiz | Técnico | Infraestructura |
| u3 | María López | maria.lopez | Técnico | Aplicaciones |
| u4 | Pedro Sánchez | pedro.sanchez | Técnico | Infraestructura |

Usuario activo inicial: **Ana García (Supervisor)**.

### Incidentes
```ts
export const SEED_INCIDENTS: Incident[] = [
  { id: 'INC-2024-0412', title: 'Fallo de conectividad en servidor de base de datos', description: 'El servicio de base de datos no responde a las peticiones de las aplicaciones internas.', status: 'En progreso', type: 'Red', priority: 'Alta', area: 'Infraestructura', classificationSource: 'Clasificado por IA', createdBy: 'ana.garcia', createdAt: '22/05/2024 08:17', assignedTo: 'carlos.ruiz', assignedAt: '22/05/2024 08:45', startedAt: '22/05/2024 09:03' },
  { id: 'INC-2024-0411', title: 'Error al generar reporte mensual de ventas', description: 'El reporte se queda cargando y no muestra resultados para el mes actual.', status: 'Asignado', type: 'Bug', priority: 'Alta', area: 'Aplicaciones', classificationSource: 'Clasificado por IA', createdBy: 'ana.garcia', createdAt: '22/05/2024 07:52', assignedTo: 'maria.lopez', assignedAt: '22/05/2024 08:20' },
  { id: 'INC-2024-0410', title: 'Usuario bloqueado después de cambio de contraseña', description: 'El usuario no puede acceder al portal corporativo después de actualizar sus credenciales.', status: 'Registrado', type: 'Acceso', priority: 'Media', area: 'Soporte a usuario', classificationSource: 'Clasificado por IA', createdBy: 'ana.garcia', createdAt: '21/05/2024 16:40' },
  { id: 'INC-2024-0409', title: 'Lentitud general en aplicación de inventario', description: 'La aplicación tarda más de un minuto en cargar cada pantalla.', status: 'Resuelto', type: 'Rendimiento', priority: 'Media', area: 'Aplicaciones', classificationSource: 'Corregida manualmente', createdBy: 'ana.garcia', createdAt: '21/05/2024 14:21', assignedTo: 'maria.lopez', solution: 'Se optimizaron las consultas principales y se amplió la memoria del servidor de aplicaciones.', resolvedAt: '21/05/2024 18:12', unindexed: true },
  { id: 'INC-2024-0408', title: 'Equipo no enciende después de actualización', description: 'El equipo de recepción no inicia correctamente después de la última actualización.', status: 'Cerrado sin solución', type: 'Hardware', priority: 'Baja', area: 'Soporte a usuario', createdBy: 'ana.garcia', createdAt: '20/05/2024 11:05', closureReason: 'No reproducible', closedAt: '20/05/2024 13:30' },
  { id: 'INC-2024-0407', title: 'Nueva solicitud sin clasificación', description: 'El usuario reporta un problema que requiere revisión del equipo de soporte.', status: 'Registrado', createdBy: 'ana.garcia', createdAt: '20/05/2024 10:14', pendingClassification: true },
];
```

### Base de conocimiento: casos similares (mock)
Incidentes históricos ya resueltos que el panel de IA muestra como referencia (`similarCasesKB` en `app/page.tsx`).

```ts
export const SIMILAR_CASES_KB: SimilarCase[] = [
  { id: 'INC-2024-0388', title: 'Fallo de acceso a APP-SRV-01', similarity: 92, type: 'Red', priority: 'Alta', area: 'Infraestructura', status: 'Resuelto', description: 'Las aplicaciones internas perdían la conexión con el servidor APP-SRV-01 de forma intermitente.', solution: 'Se reinició el servicio de red, se corrigió la regla del firewall que bloqueaba el puerto 1433 y se validó la conectividad con el balanceador.', resolvedBy: 'Carlos Ruiz', resolvedAt: '14/05/2024 17:40' },
  { id: 'INC-2024-0371', title: 'Caída del servicio de base de datos tras mantenimiento', similarity: 84, type: 'Configuración', priority: 'Alta', area: 'Infraestructura', status: 'Resuelto', description: 'Después de la ventana de mantenimiento, el servicio de base de datos dejó de aceptar conexiones.', solution: 'Se restauró la configuración previa del listener y se añadió una comprobación de conectividad al procedimiento de mantenimiento.', resolvedBy: 'Pedro Sánchez', resolvedAt: '03/05/2024 12:15' },
  { id: 'INC-2024-0352', title: 'Timeouts en aplicación de reportes', similarity: 76, type: 'Rendimiento', priority: 'Media', area: 'Aplicaciones', status: 'Resuelto', description: 'Los reportes mensuales superaban el tiempo máximo de espera y no llegaban a mostrarse.', solution: 'Se optimizó la consulta principal, se creó un índice sobre la fecha de venta y se amplió el timeout del pool de conexiones.', resolvedBy: 'María López', resolvedAt: '26/04/2024 10:05' },
  { id: 'INC-2024-0339', title: 'Accesos rechazados al portal corporativo', similarity: 68, type: 'Acceso', priority: 'Media', area: 'Soporte a usuario', status: 'Resuelto', description: 'Varios usuarios recibían error de credenciales al entrar al portal tras un cambio de contraseña.', solution: 'Se sincronizó el directorio de usuarios y se limpió la caché de sesiones del portal.', resolvedBy: 'Pedro Sánchez', resolvedAt: '18/04/2024 09:30' },
];

// Selección en el mockup: mismo área primero, luego mayor similitud, máximo 3
export const getSimilarCases = (incident: Incident) =>
  [...SIMILAR_CASES_KB]
    .filter(c => c.id !== incident.id)
    .sort((a, b) => Number(b.area === incident.area) - Number(a.area === incident.area) || b.similarity - a.similarity)
    .slice(0, 3);
```

### Constantes de catálogo
```ts
export const AREAS: TechArea[] = ['Infraestructura','Aplicaciones','Base de datos','Seguridad','Soporte a usuario'];
export const TYPES: IncidentType[] = ['Bug','Rendimiento','Acceso','Hardware','Red','Configuración'];
export const PRIORITIES: Priority[] = ['Alta','Media','Baja'];
export const STATUSES: IncidentStatus[] = ['Registrado','Asignado','En progreso','Resuelto','Cerrado sin solución'];
export const CLOSURE_REASONS: ClosureReason[] = ['Duplicado','No reproducible','Descartado'];
```

---

## 5. Reglas de negocio y permisos

### Visibilidad
- **Supervisor:** ve todos los incidentes.
- **Técnico:** solo ve incidentes con `assignedTo === currentUser.username`. Si intenta abrir uno ajeno, no se muestra el detalle.
- Un técnico **no puede** acceder a "Registrar incidente" ni a "Administración": cualquier página distinta de "Mis asignados" se fuerza a "Mis asignados". En Angular: `CanActivateFn` por rol.

### Ciclo de estados
```
Registrado ──asignar──▶ Asignado ──iniciar trabajo──▶ En progreso ──resolver──▶ Resuelto
     │                      │                              │
     └──────────────────────┴──────── cerrar sin solución ─┴──▶ Cerrado sin solución
```
- El **estado cambia automáticamente** por acciones del técnico/supervisor; no se edita a mano.
- Asignar/reasignar → `status = 'Asignado'`, guarda `assignedTo`.
- Iniciar trabajo (técnico asignado) → `En progreso` (`startedAt`).
- Resolver (técnico) → `Resuelto`. **La solución requiere al menos 30 caracteres** (texto informativo en el mockup; **no está validado en código**, implementarlo).
- Cerrar sin solución (técnico) → exige motivo (`Duplicado`, `No reproducible`, `Descartado`).
- Estados finales: `Resuelto` y `Cerrado sin solución`. En ellos no se muestran acciones de edición ni asignación.

### Asignación
- Solo se listan como candidatos técnicos con `role === 'Técnico'` **y** `area === incident.area`.
- Si el incidente no tiene área (pendiente de clasificación) o no hay técnicos en el área, se muestra aviso ámbar: *"No hay técnicos dados de alta en el área responsable."*
- Si un incidente está pendiente de clasificación, el supervisor debe clasificarlo antes de asignar.
- Título del diálogo: "Asignar incidente" o "Reasignar incidente" según exista `assignedTo`.
- **Reasignación (regla confirmada):** solo con el incidente en `Asignado` o `En progreso`, y el nuevo técnico debe pertenecer al área responsable del incidente. En `Registrado` solo se asigna por primera vez.

### Clasificación
- Fuente: `Clasificado por IA` (automática al registrar) o `Corregida manualmente` (supervisor).
- `pendingClassification = true` → tipo/prioridad/área vacíos; en tabla se muestra badge "Pendiente" y en detalle un aviso ámbar.
- Un incidente pendiente puede resolverse de dos formas: **"Reintentar clasificación IA"** (vuelve a lanzar la IA; deja `Clasificado por IA`) o **"Corregir clasificación"** (manual; deja `Corregida manualmente`). Ambas ponen `pendingClassification = false` y no cambian el estado. Ver 7.15.

### Indexación
- Incidentes `Resuelto` con `unindexed = true` muestran al supervisor el botón **"Relanzar indexación"**, que pone `unindexed = false`.

### Cambio de usuario
- Al cambiar de usuario: navega a `Incidentes` (supervisor) o `Mis asignados` (técnico) y cierra cualquier detalle abierto.

### Salvedades del mockup (decidir en Angular)
Estas cosas **no están implementadas** o solo son visuales:
- "Registrar y clasificar con IA": solo muestra mensaje fijo con id `INC-2024-0415`; **no crea el incidente** ni valida campos.
- "Guardar clasificación": solo cierra el diálogo; **no persiste** los cambios. Los selects no están enlazados.
- "Confirmar cierre": solo cierra el diálogo; no cambia el estado ni guarda el motivo.
- "Resolver": solo muestra un textarea con texto por defecto; no guarda ni cambia estado persistente.
- "Iniciar trabajo" / "Resolver": estado solo local a la pantalla de detalle (se pierde al salir).
- Editar técnico (icono lápiz) y "Cerrar sesión": sin acción.
- Notificaciones y ayuda: solo iconos.
- Las tarjetas KPI del supervisor usan **valores fijos** (18, 07, 42, 03); las del técnico se calculan parcialmente (resueltos = 12 fijo).
- Los **pasos de diagnóstico** de la IA son texto fijo. Los **casos similares** salen de una lista mock (`SIMILAR_CASES_KB`) con selección simple por área; en Angular deben venir del servicio de IA (ver 7.16 y sección 8).

---

## 6. Estructura de pantallas y rutas propuestas

| Ruta Angular | Pantalla original | Rol | Guard |
|---|---|---|---|
| `/incidentes` | Dashboard (supervisor: "Incidentes") | Supervisor | rol Supervisor |
| `/mis-asignados` | Dashboard (técnico: "Mis asignados") | Técnico | rol Técnico |
| `/incidentes/:id` | `DetailPage` | Ambos (técnico solo si es asignado) | acceso por asignación |
| `/incidentes/nuevo` | `RegisterPage` ("Registrar incidente") | Supervisor | rol Supervisor |
| `/administracion` | `AdminPage` ("Equipo técnico") | Supervisor | rol Supervisor |
| (modal global) | `UserSelector` | Ambos | — |

Redirección por defecto: `''` → `/incidentes` o `/mis-asignados` según rol del usuario activo.

### Estructura de carpetas sugerida
```
src/app/
  core/
    models/incident.models.ts
    data/seed.ts                      # usuarios, incidentes, catálogos
    services/
      auth.service.ts                 # usuario activo (signal), cambio de usuario
      incident.service.ts             # lista, filtros, asignar, iniciar, resolver, cerrar, indexar
      user.service.ts                 # técnicos, alta
      ai.service.ts                   # (mock) recomendación de diagnóstico
    guards/role.guard.ts
  layout/
    app-shell/                        # sidebar + topbar + <router-outlet>
    sidebar/
    topbar/
  shared/
    ui/
      status-badge/  priority-badge/  user-avatar/  stat-card/  empty-state/
    dialogs/
      user-selector-dialog/
  features/
    incidents/
      incident-list/                  # tabla + filtros (equivale a IncidentTable)
      incident-dashboard/             # heading + KPIs + incident-list
      incident-detail/
      assign-dialog/  classification-dialog/  close-dialog/
      ai-recommendation-panel/        # pasos + lista de casos similares desplegables (7.16)
      incident-register/
    admin/
      team-page/
      user-create-dialog/
```

---

## 7. Descripción detallada de cada componente

### 7.1 App shell
Layout `display:flex`: **Sidebar** fija (238 px) + **main** (`flex:1`). Encima, `UserSelector` como modal global.

### 7.2 Logo (en sidebar)
- Cuadro 30×30 con degradado azul `linear-gradient(145deg,#1976f3,#1252ba)`, radio 9, sombra `0 4px 10px #0d64d733`, icono `shield-check` blanco 18 px.
- Texto: **"Gestión de Incidentes TI"** (semibold, tracking tight) y debajo "Centro de operaciones de soporte" (10 px, muted).

### 7.3 Sidebar
- Alto del logo: 72 px, borde inferior.
- Etiqueta de sección (10 px, mayúsculas, `#8a96a7`): **"Gestión global"** (supervisor) o **"Mi trabajo"** (técnico).
- Ítems de navegación:
  - Supervisor: `Incidentes` (icono `clipboard-list`), `Registrar incidente` (`plus`), `Administración` (`settings`).
  - Técnico: `Mis asignados` (`user-check`).
- Estado activo: fondo `#eaf2ff`, texto `#1d65d7`, semibold. Hover: fondo `#f3f7fd`.
- Pie (`margin-top:auto`, borde superior): avatar + nombre (truncado, 12 px semibold) + `rol · área` (11 px muted); debajo botón "Cerrar sesión" (icono `log-out`).
- Se oculta por debajo de 760 px.

### 7.4 Topbar (alto 72 px, blanco, borde inferior)
- Izquierda (solo móvil): icono `menu`.
- Migas: `{Mis asignados | Incidentes}` › "Centro de operaciones" (el 2.º tramo y el chevron se ocultan en móvil).
- Derecha: botón notificaciones (`bell` con punto rojo `#f04444`), ayuda (`circle-help`), separador vertical, **selector de usuario** (avatar + nombre + rol + `chevron-down`) que abre `UserSelector`. Nombre/rol ocultos bajo 640 px.

### 7.5 UserSelector (diálogo, `max-w-xl`, sin padding)
- Título: "Seleccionar usuario activo". Descripción: "Elige el usuario con el que deseas trabajar en esta sesión."
- Lista de usuarios como tarjetas seleccionables: avatar grande (44 px), nombre, `rol · área`, `@username`, badge de rol (Supervisor morado, Técnico verde azulado) y radio personalizado.
- Pie: texto "Selecciona un usuario para continuar." + botón **Continuar** (deshabilitado sin selección) → cambia usuario y cierra.
- Preselecciona el usuario activo.

### 7.6 Dashboard
Cabecera (`page-heading`):
- Eyebrow: "Centro de operaciones" (supervisor) / "Mi espacio de trabajo" (técnico).
- H1: "Incidentes" / "Mis asignados".
- Subtítulo: "Supervisa y gestiona las incidencias de soporte de TI." / "Incidentes asignados a {fullName}."
- Solo supervisor: botón primario **"+ Nuevo incidente"** → `/incidentes/nuevo`.

**KPIs (`stats-grid`, 4 columnas; 2 en <1100 px; 1 en <430 px):**

Supervisor (valores fijos):
| Etiqueta | Valor | Ayuda | Icono | Tono |
|---|---|---|---|---|
| Incidentes abiertos | 18 | +3 esta semana | clipboard-list | blue |
| En progreso | 07 | 2 de prioridad alta | activity | amber |
| Resueltos este mes | 42 | +12% vs. mes anterior | shield-check | green |
| Pendientes de clasificación | 03 | Requieren revisión | alert-circle | red |

Técnico (parcialmente calculados, `padStart(2,'0')`):
| Etiqueta | Valor | Ayuda | Icono | Tono |
|---|---|---|---|---|
| Asignados a mí | nº de incidentes visibles | Requieren atención | user-check | blue |
| En progreso | nº con estado `En progreso` | Trabajo activo | activity | amber |
| Resueltos este mes | 12 (fijo) | Buen ritmo | shield-check | green |

Cada tarjeta: icono en cuadro 34×34 de color por tono, a la derecha "MAYO 2024" (10 px, mayúsculas), etiqueta (14 px muted), valor (24 px bold) y ayuda (11 px muted).

> Recomendación: en Angular calcular los KPIs desde los datos reales (`computed`) en vez de valores fijos.

### 7.7 IncidentTable (`incident-list`)
Tarjeta con:
- **Cabecera:** título "Todos los incidentes" (supervisor) / "Mis incidentes asignados" (técnico) + contador (pill gris con `visible.length`). Subtítulo: "Consulta y gestiona el catálogo global de incidencias." / "Solo puedes ver incidentes donde eres el técnico responsable."
- **Filtros:**
  - Buscador (icono `search`, placeholder "Buscar por título o ID..."): coincide con `title` o `id`, sin distinguir mayúsculas.
  - Select **Estado** (solo supervisor): "Todos los estados" + 5 estados.
  - Select **Prioridad**: "Todas las prioridades" + Alta/Media/Baja.
  - Botón icono `refresh-cw` "Limpiar filtros": resetea los tres.
- **Columnas:** Incidente | Estado | Clasificación | Prioridad | Área responsable | Técnico asignado | Creación | (chevron).
  - *Incidente*: icono en cuadro 28 px (`clipboard-list`, o `alert-circle` ámbar si `pendingClassification`) + título (truncado a 300 px, 12 px semibold) + id en monoespaciada 10 px.
  - *Estado*: `StatusBadge`.
  - *Clasificación*: badge ámbar "Pendiente" si pendiente; si no, `type` (texto).
  - *Prioridad*: `PriorityBadge` o "—".
  - *Área*: `area` o "Sin asignar".
  - *Técnico*: nombre completo (búsqueda por `username` en la lista de usuarios) o "—".
  - *Creación*: solo la fecha.
- Fila clicable → `/incidentes/:id`. Hover fondo `#f8fbff`. Ancho mínimo tabla 1000 px con scroll horizontal.
- **Estado vacío:** icono `search` + "No hay incidentes que coincidan con los filtros."
- Nota: el filtro de prioridad aplica también a incidentes sin prioridad (quedan excluidos al filtrar).

### 7.8 StatusBadge / PriorityBadge
| Estado | Fondo | Texto |
|---|---|---|
| Registrado | `#edf1f5` | `#5d6878` |
| Asignado | `#eaf2ff` | `#236bd2` |
| En progreso | `#fff2d9` | `#9b6100` |
| Resuelto | `#e2f6e9` | `#167847` |
| Cerrado sin solución | `#f5e8ed` | `#a34d6a` |

| Prioridad | Fondo | Texto |
|---|---|---|
| Alta | `#ffe5e3` | `#c53e39` |
| Media | `#fff0d6` | `#a96600` |
| Baja | `#e5f6eb` | `#218250` |

Badge "Pendiente": fondo `#fff2d9`, texto `#a86600`. Todos: radio 5 px, padding `3px 7px`, 10 px, semibold, sin borde.

### 7.9 UserAvatar
Círculo con iniciales (primeras letras de las dos primeras palabras... en realidad `name.split(' ').map(n=>n[0]).join('').slice(0,2)`). Fondo `#e0edff`, texto `#1b61cf`, borde `#c6dcfd`, 11 px bold. Variante grande: 44 px. Si no hay usuario, usa "Administrador".

### 7.10 IncidentDetail (`/incidentes/:id`)
- Enlace "← Volver a incidentes" (`arrow-left`, azul `#326fd0`).
- Cabecera: `id` (monoespaciada) + `StatusBadge` (estado efectivo), H1 con título, subtítulo *"Creado el {createdAt} por Ana García · {Asignado a {nombre} | Sin técnico asignado}"* (el nombre "Ana García" está fijo en el mockup; usar `createdBy`).
- **Botones según rol/estado** (solo si el estado NO es final):
  - Supervisor: `Reintentar clasificación IA` (outline, icono `refresh-cw`, **solo si `pendingClassification`**, ver 7.15) → lanza la clasificación; `Corregir clasificación` (outline, icono `pencil`) → abre diálogo; `Asignar`/`Reasignar` (primario, icono `user-check`) → abre `AssignDialog`. Orden de izquierda a derecha: Reintentar · Corregir · Asignar.
  - Técnico asignado: `Iniciar trabajo` (icono `zap`, solo si no ha iniciado), `Analizar con IA` (outline, icono `sparkles`), `Resolver` (icono `shield-check`, solo tras iniciar), `Cerrar sin solución` (ghost).
  - Estado "En progreso" ya trae `workStarted = true`.
- **Rejilla** 2 columnas (`1.1fr / .9fr`, 1 columna en <760 px):
  - **Columna izquierda:**
    - Tarjeta "Detalle del incidente": bloque *Descripción*; separador; bloque *Clasificación* (badges `type` con icono `tag`, prioridad, `area` con icono `hard-drive`, o aviso ámbar si pendiente; debajo la fuente de clasificación o "Pendiente de clasificación"); si `resolved`: tarjeta verde "Incidente resuelto" con nota "La solución requiere al menos 30 caracteres y quedará registrada en el historial." + textarea.
    - Supervisor + `unindexed`: botón outline "Relanzar indexación" (`refresh-cw`).
  - **Columna derecha:**
    - Técnico asignado: tarjeta **IA** (ver 7.11).
    - Resto: tarjeta "Acciones del supervisor" con texto: *"Puedes corregir la clasificación, asignar al técnico del área responsable y relanzar la indexación. El estado cambia automáticamente por las acciones del técnico."*

### 7.11 Panel de IA (`ai-recommendation-panel`)
- Cabecera: icono `bot` (cuadro azul 31 px), título "Recomendación de diagnóstico", subtítulo "Asistencia para tu análisis técnico", badge "IA" a la derecha. Borde `#cfddf4`.
- **Estado bloqueado** (antes de pulsar "Analizar con IA"): icono `sparkles` + "Pulsa “Analizar con IA” para obtener pasos de diagnóstico y casos similares."
- **Estado abierto:** "Basado en análisis de patrones y casos similares:" + 3 pasos numerados (círculo azul con número):
  1. Verificar estado del servicio y del servidor
  2. Comprobar conectividad y puertos
  3. Revisar logs de aplicación y balanceador
  
  (Cada paso con la misma descripción: "Valida la conectividad, revisa recursos y contrasta el comportamiento reciente.")
  Luego separador, "Casos similares usados (N) · pulsa un caso para ver el detalle" y la lista de **casos similares desplegables** (ver 7.16). Cierre con callout azul: *"Esta es una sugerencia. La decisión final es del técnico."*
- El modelo `AIRecommendation` (steps, referenceIncidents, loading, error) ya está previsto para conectar un servicio real; implementar estados de carga y error.

### 7.12 Diálogos del detalle
- **AssignDialog** (`max-w-lg`): título dinámico; descripción *"Solo se muestran técnicos del área responsable: {area | sin área}."*; lista de técnicos candidatos (avatar, nombre, `{username}@empresa.es`, radio); aviso ámbar si no hay; botones `Cancelar` / `Asignar` (deshabilitado sin selección). Preselecciona el técnico actual.
- **Corregir clasificación:** descripción "Actualiza los datos antes de asignar el incidente."; selects *Tipo*, *Prioridad*, *Área responsable* (valores por defecto = actuales); botones `Cancelar` / `Guardar clasificación`. Al guardar debería poner `classificationSource = 'Corregida manualmente'` y `pendingClassification = false`.
- **Cerrar sin solución:** descripción "Selecciona un motivo para registrar el cierre del incidente."; select "Motivo de cierre" (Duplicado/No reproducible/Descartado); botones `Cancelar` / `Confirmar cierre` (variante destructiva, deshabilitar sin motivo).

### 7.13 IncidentRegister (`/incidentes/nuevo`)
- Ancho máximo 1000 px. Eyebrow "Nuevo registro", H1 "Registrar incidente", subtítulo "Describe el problema para que podamos ayudarte."
- Tarjeta "Detalles del incidente" + texto "La IA clasificará automáticamente el tipo, prioridad y área responsable."
- Campos: **Título \*** (placeholder "Ej.: Error 500 al intentar generar reporte de ventas"), **Descripción \*** (textarea, placeholder "Explica el problema con el mayor detalle posible. No escribas contraseñas ni datos sensibles.", mín. 144 px).
- Aviso con icono `alert-triangle` ámbar: "No incluyas contraseñas, tokens ni información sensible."
- Botón ancho completo: **"Registrar y clasificar con IA"** (icono `sparkles`).
- Tras enviar: tarjeta verde con `shield-check` — *"Incidente registrado correctamente"* / *"Se ha creado el incidente INC-2024-0415 y fue clasificado por IA."* (id fijo en mockup; generar correlativo real).
- En Angular: formulario reactivo con `Validators.required` en ambos campos; sugerencia de mínimos de longitud.

### 7.14 Administración (`/administracion`, "Equipo técnico")
- Eyebrow "Administración", H1 "Equipo técnico", subtítulo "Gestiona los técnicos disponibles para asignar incidentes." + botón **"+ Dar de alta técnico"**.
- Tarjeta "Técnicos registrados" con contador. Filas: avatar grande, nombre, `{username}@empresa.es · {area}`, badge "Técnico", botón icono lápiz (`Editar {nombre}`, sin acción).
- **UserCreateDialog** "Dar de alta técnico" — descripción "Registra un técnico para que pueda recibir incidentes de su área."; campos **Nombre completo \*** (ej. "Laura Martínez"), **Usuario corporativo \*** (ej. "laura.martinez"), **Área técnica \*** (select de 5 áreas). Botón `Crear técnico` deshabilitado hasta completar los tres. Crea `{id:'u'+Date.now(), role:'Técnico', ...}` y limpia el formulario.
- Sugerencia: validar unicidad del `username`.

### 7.15 Botón "Reintentar clasificación IA" (detalle del incidente, supervisor)
Añadido al mockup (`DetailPage` en `app/page.tsx`); la spec lo pedía y no existía.

- **Cuándo se muestra:** rol Supervisor + `incident.pendingClassification === true` + incidente no cerrado (`canSupervisorEdit`). Con la clasificación ya hecha desaparece.
- **Posición:** primer botón del grupo de acciones del supervisor, antes de "Corregir clasificación" y "Asignar".
- **Aspecto:** variante `outline`, icono `refresh-cw` (a la izquierda), texto **"Reintentar clasificación IA"**.
- **Estados:**
  | Estado | Aspecto |
  |---|---|
  | Reposo | Texto "Reintentar clasificación IA", habilitado |
  | Cargando | Texto **"Clasificando con IA..."**, icono girando (`animate-spin`), botón deshabilitado. Dura 1,5 s en el mockup (`setTimeout`, simula la llamada a la IA) |
  | Éxito | El botón desaparece; el aviso ámbar "Pendiente de clasificación…" se sustituye por los badges de tipo, prioridad y área; el texto de fuente pasa a "Clasificado por IA"; la fila de la tabla deja de mostrar el badge "Pendiente" |
  | Error | **No simulado en el mockup.** En Angular: mantener `pendingClassification = true`, volver al estado Reposo y mostrar un mensaje de error (p. ej. "No se pudo clasificar. Inténtalo de nuevo o corrige la clasificación manualmente.") |
- **Efecto sobre los datos (éxito):**
  ```ts
  { ...incident,
    type, priority, area,                       // resultado de la IA
    classificationSource: 'Clasificado por IA',
    pendingClassification: false }
  ```
  El mockup usa un resultado **fijo**: `type: 'Acceso'`, `priority: 'Media'`, `area: 'Soporte a usuario'`. En Angular debe venir del servicio de IA. El estado del incidente **no cambia** (sigue `Registrado`); tras clasificar ya se puede asignar a un técnico del área resultante.
- **Implementación en React (referencia):** `DetailPage` recibe la prop `onRetryClassification`; guarda `retrying` con `useState`, y al pulsar hace `setRetrying(true)` → tras 1,5 s llama a `onRetryClassification()` y `setRetrying(false)`. `Page` implementa el callback actualizando `incidentList` y `selectedIncident`.
- **Equivalente Angular:**
  ```ts
  // incident-detail.component.ts
  retrying = signal(false);
  canRetry = computed(() => this.auth.isSupervisor() && !!this.incident().pendingClassification && !this.isFinal());
  async retryClassification() {
    this.retrying.set(true);
    try { await this.incidents.retryClassification(this.incident().id); }
    catch { this.error.set('No se pudo clasificar…'); }
    finally { this.retrying.set(false); }
  }
  // incident.service.ts
  async retryClassification(id: string) {
    const r = await firstValueFrom(this.ai.classify(this.byId(id)!)); // { type, priority, area }
    this.patch(id, { ...r, classificationSource: 'Clasificado por IA', pendingClassification: false });
  }
  ```
  ```html
  @if (canRetry()) {
    <button class="btn-outline" [disabled]="retrying()" (click)="retryClassification()">
      <i-lucide name="refresh-cw" [class.animate-spin]="retrying()" />
      {{ retrying() ? 'Clasificando con IA...' : 'Reintentar clasificación IA' }}
    </button>
  }
  ```
- **Datos de prueba:** el incidente `INC-2024-0407` ("Nueva solicitud sin clasificación") arranca con `pendingClassification: true`; sirve para probar el botón con el usuario Ana García (Supervisor).

### 7.16 Casos similares con detalle desplegable (panel de IA, técnico)
Sustituye al ítem estático `INC-2024-0412 · Fallo de acceso a APP-SRV-01 · 92%` que traía el mockup original. Implementado en `DetailPage` (`app/page.tsx`).

- **Dónde:** dentro de la tarjeta de IA, una vez pulsado "Analizar con IA", bajo el título **"Casos similares usados (N) · pulsa un caso para ver el detalle"**.
- **Lista:** hasta 3 casos (`getSimilarCases(incident)`: primero los del mismo área del incidente, después por mayor similitud; nunca el propio incidente). Cada caso es una fila-botón con:
  - icono `clipboard-list`,
  - **título** (truncado) y debajo el **ID** en monoespaciada,
  - **pill de similitud** (`92%`, fondo `#e9f1ff`, texto `#266cd0`, bold),
  - chevron `chevron-down` que rota 180° al abrir.
- **Interacción (acordeón en línea):**
  - Al pulsar una fila se **despliega su detalle justo debajo, en el mismo panel**. No hay navegación, no se abre otra pestaña ni otra ruta, y el resto del incidente sigue visible.
  - **Solo un caso abierto a la vez** (estado `openCase: string | null`). Pulsar otro cierra el anterior; pulsar el mismo lo cierra.
  - Accesibilidad: la fila es un `<button type="button">` con `aria-expanded`; operable con teclado (Enter/Espacio).
- **Contenido del detalle desplegado** (fondo `#f8fbff`, borde `#c9daf5`, se pega a la fila con las esquinas inferiores redondeadas):
  1. Fila de badges: estado (`StatusBadge`, "Resuelto"), tipo (con icono `tag`), prioridad (`PriorityBadge`), área (con icono `hard-drive`).
  2. **Descripción** (etiqueta `field-label`, texto muted).
  3. **Solución aplicada** (etiqueta `field-label`, texto normal).
  4. Pie: "Resuelto por {resolvedBy} · {resolvedAt}" (10 px muted).
- **Reglas:**
  - El detalle es **solo lectura**; no hay acciones sobre el caso similar.
  - Solo lo ve el técnico asignado (forma parte del panel de IA, que el supervisor no tiene).
  - Al salir del detalle, el estado `openCase` se reinicia (es estado local del componente).
- **Datos:** ver "Base de conocimiento: casos similares" en la sección 4 y el modelo `SimilarCase` en la sección 3.
- **Equivalente Angular:**
  ```ts
  // ai-recommendation-panel.component.ts
  cases = input.required<SimilarCase[]>();
  openCase = signal<string | null>(null);
  toggle(id: string) { this.openCase.update(cur => cur === id ? null : id); }
  ```
  ```html
  <p class="similar-title">Casos similares usados ({{ cases().length }}) · pulsa un caso para ver el detalle</p>
  <div class="flex flex-col gap-2">
    @for (c of cases(); track c.id) {
      <div class="reference-card" [class.open]="openCase() === c.id">
        <button type="button" class="reference-item"
                [attr.aria-expanded]="openCase() === c.id" (click)="toggle(c.id)">
          <i-lucide name="clipboard-list" />
          <div class="min-w-0 flex-1 text-left">
            <p class="truncate font-semibold">{{ c.title }}</p>
            <p class="mono">{{ c.id }}</p>
          </div>
          <span class="similarity-pill">{{ c.similarity }}%</span>
          <i-lucide name="chevron-down" class="chevron" [class.rotate-180]="openCase() === c.id" />
        </button>
        @if (openCase() === c.id) {
          <div class="reference-detail">
            <div class="flex flex-wrap gap-1.5">
              <app-status-badge [status]="c.status" />
              <span class="badge-outline"><i-lucide name="tag" />{{ c.type }}</span>
              <app-priority-badge [priority]="c.priority" />
              <span class="badge-outline"><i-lucide name="hard-drive" />{{ c.area }}</span>
            </div>
            <div><p class="field-label">Descripción</p><p>{{ c.description }}</p></div>
            <div><p class="field-label">Solución aplicada</p><p>{{ c.solution }}</p></div>
            <p class="foot">Resuelto por {{ c.resolvedBy }} · {{ c.resolvedAt }}</p>
          </div>
        }
      </div>
    }
  </div>
  ```
  Con `MatExpansionPanel` (Angular Material) se obtiene el mismo comportamiento con `<mat-accordion>` (un solo panel abierto por defecto).
- **Pruebas:** con el usuario Carlos Ruiz (Técnico), abrir `INC-2024-0412` (En progreso) → "Analizar con IA" → aparecen 3 casos (los de Infraestructura primero) → pulsar cada uno.
- **Estados por definir con backend:** carga de los casos (`loading`), lista vacía ("No se encontraron casos similares") y error.

---

## 8. Servicios y estado en Angular (esqueleto)

```ts
// auth.service.ts
@Injectable({ providedIn: 'root' })
export class AuthService {
  private users = inject(UserService);
  readonly currentUser = signal<User>(SEED_USERS[0]);
  readonly isSupervisor = computed(() => this.currentUser().role === 'Supervisor');
  switchUser(u: User) { this.currentUser.set(u); /* + router.navigate según rol */ }
}

// incident.service.ts
@Injectable({ providedIn: 'root' })
export class IncidentService {
  private auth = inject(AuthService);
  readonly incidents = signal<Incident[]>(SEED_INCIDENTS);
  readonly visible = computed(() =>
    this.auth.isSupervisor()
      ? this.incidents()
      : this.incidents().filter(i => i.assignedTo === this.auth.currentUser().username));

  byId(id: string) { return this.incidents().find(i => i.id === id); }
  assign(id: string, username: string) { this.patch(id, { assignedTo: username, status: 'Asignado' }); }
  start(id: string)   { this.patch(id, { status: 'En progreso' }); }
  resolve(id: string, solution: string) { this.patch(id, { status: 'Resuelto', solution }); }
  close(id: string, reason: ClosureReason) { this.patch(id, { status: 'Cerrado sin solución', closureReason: reason }); }
  reindex(id: string) { this.patch(id, { unindexed: false }); }
  async retryClassification(id: string) { /* ver 7.15: llama a ai.classify() y aplica el resultado */ }
  private patch(id: string, p: Partial<Incident>) {
    this.incidents.update(list => list.map(i => i.id === id ? { ...i, ...p } : i));
  }
}
```

Filtros de la tabla con signals:
```ts
search = signal(''); status = signal('all'); priority = signal('all');
filtered = computed(() => this.service.visible().filter(i =>
  (!this.search() || i.title.toLowerCase().includes(this.search().toLowerCase()) || i.id.toLowerCase().includes(this.search().toLowerCase())) &&
  (this.priority() === 'all' || i.priority === this.priority()) &&
  (this.status() === 'all' || i.status === this.status())));
```

Guard de rol:
```ts
export const roleGuard = (role: UserRole): CanActivateFn => () =>
  inject(AuthService).currentUser().role === role || inject(Router).createUrlTree(['/']);
```

---

## 9. Diseño visual (design tokens)

### Tipografía
`font-family: Arial, Helvetica, sans-serif`. H1 de página: 25 px, bold, `letter-spacing:-.04em`. Cuerpo de tablas/etiquetas: 10–13 px.

### Variables de color (definidas en `oklch`)
```css
:root {
  --background: oklch(.985 .004 245);
  --foreground: oklch(.22 .025 250);
  --card: oklch(1 0 0);            --card-foreground: oklch(.22 .025 250);
  --popover: oklch(1 0 0);         --popover-foreground: oklch(.22 .025 250);
  --primary: oklch(.55 .19 255);   --primary-foreground: oklch(1 0 0);
  --secondary: oklch(.96 .008 245);--secondary-foreground: oklch(.3 .03 250);
  --muted: oklch(.965 .006 245);   --muted-foreground: oklch(.51 .03 250);
  --accent: oklch(.95 .025 245);   --accent-foreground: oklch(.3 .05 255);
  --destructive: oklch(.59 .2 27);
  --border: oklch(.91 .012 245);   --input: oklch(.89 .015 245);
  --ring: oklch(.55 .19 255);
  --radius: .65rem;
}
```
Solo existe tema claro (`.dark` no redefine nada). El layout declara `colorScheme: 'light dark'` y `themeColor` blanco/negro; puede ignorarse.

### Colores auxiliares recurrentes
Azul de énfasis `#1d65d7` / `#3175da`; bordes de tarjeta `#e0e6ef`; sombra `0 2px 8px #16294b08`; texto secundario `#5a6675`, `#637083`, `#8a96a7`; aviso ámbar: fondo `amber-50`, borde `amber-200`, texto `amber-800`; éxito: fondo `#f4fbf6`, borde `#c8e7d2`.

### Breakpoints
- `≤1100 px`: KPIs a 2 columnas, paddings reducidos.
- `≤760 px`: sidebar oculto, topbar 62 px, icono menú visible, cabeceras en columna, detalle a 1 columna, buscador a ancho completo.
- `≤430 px`: KPIs a 1 columna, se oculta nombre en el selector de usuario.

### Iconos (Lucide)
Activity, AlertCircle, AlertTriangle, ArrowLeft, Bell, Bot, ChevronDown, ChevronRight, CircleHelp, ClipboardList, Filter, HardDrive, LayoutDashboard (importado, sin uso), ListFilter, LogOut, Menu, Pencil, Plus, RefreshCw, Search, Settings, ShieldCheck, Sparkles, Tag, UserCheck, Users (sin uso), X (sin uso), Zap.

### Iconos/activos
`public/`: `icon.svg`, `icon-light-32x32.png`, `icon-dark-32x32.png`, `apple-icon.png` (favicon), y placeholders sin uso. Copiar los iconos a `src/assets/` y enlazar en `index.html`. Título: **Gestión de Incidentes TI**; descripción: "Centro de operaciones para registrar, clasificar y resolver incidentes de soporte de TI."

---

## 10. Estilos personalizados (CSS completo del mockup)

Clases propias definidas en `app/globals.css`. Se pueden copiar a `styles.scss` (o al `styles` de cada componente). Sustituciones necesarias al salir de Tailwind: la regla `* { @apply border-border outline-ring/50 }` equivale a `* { border-color: var(--border); outline-color: color-mix(in oklch, var(--ring) 50%, transparent); }` y `body { @apply bg-background text-foreground }` a `body { background: var(--background); color: var(--foreground); }`.

```css
* { border-color: var(--border); outline-color: color-mix(in oklch, var(--ring) 50%, transparent); }
body { background: var(--background); color: var(--foreground); font-family: Arial, Helvetica, sans-serif; }
button { cursor: pointer; }
.app-shell { min-height: 100vh; display: flex; background: var(--background); }
.app-sidebar { width: 238px; flex: none; background: white; border-right: 1px solid var(--border); display: flex; flex-direction: column; }
.sidebar-logo { height: 72px; padding: 0 20px; display: flex; align-items: center; border-bottom: 1px solid var(--border); }
.brand-mark { width: 30px; height: 30px; border-radius: 9px; display: grid; place-items: center; color: white; background: linear-gradient(145deg, #1976f3, #1252ba); box-shadow: 0 4px 10px #0d64d733; }
.brand-mark svg { width: 18px; }
.nav-section { font-size: 10px; font-weight: 700; color: #8a96a7; text-transform: uppercase; letter-spacing: .08em; padding: 0 12px 10px; }
.nav-item { width: 100%; display: flex; align-items: center; gap: 11px; padding: 10px 12px; margin-bottom: 3px; border-radius: 8px; color: #5a6675; font-size: 13px; text-align: left; transition: .15s ease; }
.nav-item svg { width: 16px; height: 16px; }
.nav-item:hover { background: #f3f7fd; color: #1d65d7; }
.nav-item.active { color: #1d65d7; background: #eaf2ff; font-weight: 600; }
.main-shell { min-width: 0; flex: 1; }
.topbar { height: 72px; display: flex; align-items: center; padding: 0 30px; background: #fff; border-bottom: 1px solid var(--border); }
.mobile-brand { display: none; }
.topbar-context { display: flex; align-items: center; gap: 8px; }
.icon-button { position: relative; color: #637083; padding: 5px; }
.icon-button svg { width: 18px; }
.notification-dot { position: absolute; top: 3px; right: 3px; width: 5px; height: 5px; border-radius: 50%; background: #f04444; border: 1px solid white; }
.user-switcher { display: flex; align-items: center; gap: 9px; }
.avatar-blue { background: #e0edff; color: #1b61cf; border: 1px solid #c6dcfd; }
.avatar-blue > span { font-size: 11px; font-weight: 700; }
.page-wrap { max-width: 1440px; margin: 0 auto; padding: 34px 36px 52px; }
.narrow-wrap { max-width: 1000px; }
.page-heading { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 26px; }
.page-heading h1 { font-size: 25px; font-weight: 700; letter-spacing: -.04em; }
.subtitle { margin-top: 6px; color: var(--muted-foreground); font-size: 13px; }
.eyebrow { margin-bottom: 7px; font-size: 10px; color: #3175da; font-weight: 700; text-transform: uppercase; letter-spacing: .11em; }
.surface-card, .stat-card { border: 1px solid #e0e6ef; box-shadow: 0 2px 8px #16294b08; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 22px; }
.stat-icon { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 9px; }
.stat-icon svg { width: 17px; }
.stat-icon.blue { color: #1669dc; background: #e8f1ff; }
.stat-icon.amber { color: #b46a00; background: #fff2da; }
.stat-icon.green { color: #18834b; background: #e3f7ec; }
.stat-icon.red { color: #d13d3d; background: #ffebeb; }
.search-box { position: relative; width: 230px; }
.search-box svg { position: absolute; z-index: 1; left: 10px; top: 9px; width: 15px; color: #8390a1; }
.search-box input { height: 34px; padding-left: 32px; font-size: 12px; }
.filter-select { height: 34px; min-width: 145px; font-size: 12px; }
.filter-select svg { width: 14px; color: #778496; }
.incident-table { width: 100%; min-width: 1000px; border-collapse: collapse; }
.incident-table th { padding: 12px 14px; background: #fafbfd; color: #7a8798; font-size: 10px; font-weight: 700; text-align: left; text-transform: uppercase; letter-spacing: .04em; border-bottom: 1px solid var(--border); }
.incident-table td { padding: 13px 14px; border-bottom: 1px solid #eef1f5; vertical-align: middle; }
.incident-table tbody tr { transition: background .15s; cursor: pointer; }
.incident-table tbody tr:hover { background: #f8fbff; }
.incident-icon { width: 28px; height: 28px; display: grid; place-items: center; border-radius: 7px; background: #eaf2ff; color: #3479dd; flex: none; }
.incident-icon svg { width: 14px; }
.incident-icon.pending { color: #be7700; background: #fff3da; }
.status-badge, .priority-badge, .pending-badge { border: 0; border-radius: 5px; padding: 3px 7px; font-size: 10px; font-weight: 600; white-space: nowrap; }
.status-registered { background: #edf1f5; color: #5d6878; }
.status-assigned { background: #eaf2ff; color: #236bd2; }
.status-progress { background: #fff2d9; color: #9b6100; }
.status-resolved { background: #e2f6e9; color: #167847; }
.status-closed { background: #f5e8ed; color: #a34d6a; }
.priority-high { background: #ffe5e3; color: #c53e39; }
.priority-medium { background: #fff0d6; color: #a96600; }
.priority-low { background: #e5f6eb; color: #218250; }
.pending-badge { background: #fff2d9; color: #a86600; }
.empty-state { display: flex; flex-direction: column; gap: 8px; align-items: center; justify-content: center; padding: 64px; color: var(--muted-foreground); font-size: 13px; }
.empty-state svg { width: 28px; }
.back-link { display: flex; align-items: center; gap: 6px; color: #326fd0; font-size: 12px; font-weight: 600; margin-bottom: 20px; }
.back-link svg { width: 15px; }
.detail-heading { align-items: flex-start; }
.detail-heading h1 { max-width: 700px; }
.incident-id { color: #69809f; font-size: 11px; font-family: monospace; font-weight: 600; }
.detail-grid { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(340px, .9fr); gap: 18px; }
.field-label { color: #6c7b8e; font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: .07em; }
.body-copy { color: #455365; font-size: 13px; line-height: 1.7; margin-top: 9px; }
.ai-card { border: 1px solid #cfddf4; box-shadow: 0 4px 16px #2970c80a; }
.ai-card .ai-icon, .ai-icon { width: 31px; height: 31px; display: grid; place-items: center; border-radius: 9px; color: #1868da; background: #e6f0ff; }
.ai-icon svg { width: 16px; }
.ai-badge { background: #e9f1ff; color: #266cd0; border: 0; font-size: 10px; }
.ai-callout { display: flex; align-items: flex-start; gap: 9px; border-radius: 8px; background: #eff6ff; color: #2b68bd; padding: 10px; font-size: 11px; line-height: 1.45; }
.ai-callout svg { width: 16px; flex: none; }
.ai-step { display: flex; gap: 12px; }
.ai-step > span { display: grid; place-items: center; width: 21px; height: 21px; flex: none; background: #1768d9; color: white; border-radius: 50%; font-size: 11px; font-weight: 700; }
.ai-step + .ai-step { padding-top: 15px; border-top: 1px solid #e9edf2; }
.reference-item { display: flex; align-items: center; gap: 9px; width: 100%; text-align: left; background: #fff; border: 1px solid #e5eaf0; padding: 10px; border-radius: 7px; font-size: 11px; transition: .15s ease; }
.reference-item svg { width: 14px; flex: none; color: #4c86df; }
.reference-item:hover { background: #f8fbff; border-color: #c9daf5; }
.reference-item .chevron { color: #8390a1; transition: transform .2s ease; }
.similarity-pill { flex: none; border-radius: 999px; background: #e9f1ff; color: #266cd0; padding: 2px 7px; font-size: 10px; font-weight: 700; }
.reference-card.open .reference-item { border-color: #c9daf5; border-bottom-left-radius: 0; border-bottom-right-radius: 0; background: #f8fbff; }
.reference-detail { display: flex; flex-direction: column; gap: 11px; border: 1px solid #c9daf5; border-top: 0; border-radius: 0 0 7px 7px; background: #f8fbff; padding: 12px; font-size: 11px; }
.ai-locked { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 40px 20px; text-align: center; font-size: 12px; color: var(--muted-foreground); }
.ai-locked svg { width: 28px; color: #99a8ba; }
.user-option { display: flex; align-items: center; gap: 12px; border: 1px solid #e3e8ef; border-radius: 8px; padding: 11px 13px; text-align: left; transition: .15s ease; }
.user-option:hover, .user-option.selected { border-color: #7baaf0; background: #f3f8ff; }
.radio-dot { width: 17px; height: 17px; border: 1.5px solid #c4ccd7; border-radius: 50%; flex: none; }
.radio-dot.checked { border: 5px solid #2a77de; }
.role-tech { background: #e1f5f1; color: #187c72; border: 0; }
.role-supervisor { background: #eae3ff; color: #6b43b3; border: 0; }
.form-stack { display: flex; flex-direction: column; gap: 11px; }
.form-stack label { display: flex; flex-direction: column; gap: 7px; color: #425166; font-size: 12px; font-weight: 600; }
.required { color: #d34040; }
.success-card { border-color: #c8e7d2; background: #f4fbf6; margin-top: 16px; }
.success-icon { display: grid; place-items: center; width: 32px; height: 32px; color: #178344; background: #dff4e5; border-radius: 50%; }
.success-icon svg { width: 18px; }
.team-row { display: flex; align-items: center; gap: 12px; padding: 15px 20px; border-bottom: 1px solid #eef1f5; }
.team-row:last-child { border-bottom: 0; }

@media (max-width: 1100px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .page-wrap { padding: 28px 22px; } .topbar { padding: 0 22px; } }
@media (max-width: 760px) {
  .app-sidebar { display: none; }
  .topbar { height: 62px; padding: 0 16px; }
  .mobile-brand { display: block; margin-right: 12px; } .mobile-brand svg { width: 19px; }
  .topbar-context { font-size: 12px; } .topbar-context span:last-child, .topbar-context svg { display: none; }
  .page-wrap { padding: 24px 15px 40px; }
  .page-heading { align-items: flex-start; flex-direction: column; } .page-heading > button { align-self: stretch; }
  .stats-grid { grid-template-columns: 1fr 1fr; gap: 10px; }
  .detail-grid { grid-template-columns: 1fr; }
  .search-box { width: 100%; } .filter-select { flex: 1; min-width: 0; }
}
@media (max-width: 430px) { .stats-grid { grid-template-columns: 1fr; } .user-switcher span { display: none; } }
```

> Ajustes menores omitidos por brevedad de móvil: `.stat-card` con padding 12 px, etiqueta 11 px y valor 20 px en ≤760 px; y los botones de acción del detalle ocupan el ancho (`flex:1`, `flex-wrap`).

### Estilos de los componentes base (shadcn)
Si se usa Angular Material/PrimeNG hay que **igualar** estas medidas para conservar el aspecto:
- **Button:** alto 32 px (`sm` 28, `lg` 36, `icon` 32×32), radio `lg` (`--radius`), 14 px, medium. Variantes: `default` (fondo `--primary`), `outline` (borde `--border`, fondo blanco, hover `--muted`), `ghost` (hover `--muted`), `destructive` (fondo `destructive/10`, texto `--destructive`), `secondary`, `link`. Foco: anillo de 3 px `ring/50`. Deshabilitado: opacidad 50 %.
- **Input:** alto 32 px, radio `lg`, borde `--input`, padding `4px 10px`, 14 px. **Textarea:** mín. 64 px, mismo estilo.
- **Card:** fondo `--card`, radio `xl`, `ring 1px foreground/10`, padding vertical 16 px; header con padding horizontal 16 px; título 16 px medium.
- **Badge:** alto 20 px, pill (`rounded-4xl`), 12 px, medium, padding `2px 8px`. Variantes `default`, `secondary`, `destructive`, `outline`, `ghost`, `link`.
- **Dialog / Select / Avatar / Separator:** basados en `@base-ui/react` (ver `components/ui/dialog.tsx`, `select.tsx`, `avatar.tsx`). Diálogo centrado con overlay; Select con trigger, lista desplegable e ítems con check.
- **Separator:** línea de 1 px `--border` (horizontal o vertical).

---

## 11. Plan de migración sugerido (orden)

1. Crear proyecto Angular, instalar `lucide-angular` y (opcional) Tailwind / Angular Material.
2. Copiar modelos, catálogos y datos semilla (secciones 3–4).
3. Pegar variables y CSS global (sección 10).
4. Crear `AuthService`, `IncidentService`, `UserService` con signals (sección 8).
5. Construir componentes compartidos: `status-badge`, `priority-badge`, `user-avatar`, `stat-card`, `empty-state`.
6. Construir `app-shell` (sidebar + topbar) y las rutas con guards.
7. `UserSelector` como diálogo global.
8. `IncidentList` + filtros → `Dashboard`.
9. `IncidentDetail` + diálogos (asignar, clasificación, cierre) + panel IA.
10. `IncidentRegister` y `Admin` (con `UserCreateDialog`).
11. **Completar lo no implementado** (sección 5, "Salvedades") y validaciones.
12. Pruebas responsive (1100 / 760 / 430 px) y accesibilidad (`aria-label` de botones de icono, foco visible, cierre de diálogos con Esc).

---

## 12. Pendientes y decisiones abiertas

- **Backend/API:** no existe. Definir endpoints REST (incidentes, usuarios, clasificación IA, indexación) o mantener mocks.
- **Autenticación real:** el selector de usuario es solo una simulación; "Cerrar sesión" no hace nada.
- **IA:** clasificación automática al registrar, **reintento de clasificación** (7.15) y recomendación de diagnóstico (pasos y **casos similares**, 7.16) están simuladas con datos fijos/mock. Definir servicio real (`ai.classify()`, `ai.recommend()` que devuelva `SimilarCase[]`; criterio de similitud y nº máximo de casos), estados `loading` / `error`, y el proceso de indexación (`unindexed`).
- **Validaciones:** longitud mínima de solución (30 caracteres), título/descripción obligatorios, unicidad de `username`.
- **Fechas:** pasar de string a `Date`/ISO; `createdBy` se muestra fijo como "Ana García".
- **KPIs reales** calculados desde datos, con rango de fechas (el mockup fija "Mayo 2024").
- **Historial:** el mockup menciona que la solución "quedará registrada en el historial", pero no hay pantalla de historial/auditoría.
- **Notificaciones, ayuda y edición de técnicos:** sin funcionalidad.
- **Tema oscuro:** no implementado.
- **Accesibilidad:** filas de tabla clicables sin soporte de teclado; añadir `tabindex`, `role="link"` o un enlace real en el título.
- **Correlativo de ID:** definir generación (`INC-AAAA-NNNN`), normalmente en backend.

---

## 13. Vistas por rol

Ficha técnica de **cada vista** de la aplicación, agrupada por rol. Está pensada para usarse **vista por vista**: se le pasa a la IA la captura del diseño y este archivo, y se le indica *"lee el punto 13.X.Y, ahí está la información de esta vista"*.

> **Precedencia:** las capturas mandan en lo **visual** (espaciados, colores exactos, proporciones); este documento manda en **comportamiento, datos, textos y reglas**. Si algo de una captura contradice una regla de esta sección, se sigue la regla y se anota la diferencia.
> Las secciones 1.1 (permisos), 3–4 (modelos y datos), 5 (reglas), 9–10 (tokens y CSS) y 7.x (detalle de componentes) son la base; aquí se resumen y se enlazan.

### 13.1 Cómo usar esta sección

**Proceso recomendado por vista**
1. Adjuntar la captura de la vista (ver nombre sugerido en el índice).
2. Adjuntar `MIGRACION_ANGULAR.md`.
3. Prompt tipo:
   > *"Implementa la vista **13.4.1 (S1 · Listado global de incidentes)** en Angular. Usa la captura adjunta como referencia visual y la ficha 13.4.1 de `MIGRACION_ANGULAR.md` como fuente de verdad para datos, textos, reglas y componentes. Reutiliza los componentes del catálogo 13.2 (si no existen todavía, créalos primero). Usa el modelo y datos de las secciones 3 y 4, y los estilos de las secciones 9 y 10."*
4. Verificar contra los **criterios de aceptación** de la ficha.

**Orden sugerido de implementación:** 13.2 (catálogo reutilizable) → 13.3 (selector) → shell (sidebar/topbar, secciones 7.1–7.4) → T1/S1 (listados) → S2/T2 (detalles) → diálogos → S5/S6.

**Estructura de cada ficha:** Ruta y acceso · Objetivo · Wireframe · Componentes que aparecen · Reutilizables usados · Específicos de la vista · Datos y estado · Interacciones · Reglas de negocio · Textos exactos · Estados especiales · Responsive · Criterios de aceptación · Notas.

**Índice de vistas**

| ID | Vista | Rol | Ruta / tipo | Ficha | Captura sugerida |
|---|---|---|---|---|---|
| V0 | Selector de usuario activo | Ambos | modal global | 13.3 | `00-selector-usuario.png` |
| S1 | Listado global de incidentes | Supervisor | `/incidentes` | 13.4.1 | `s1-listado-global.png` |
| S2 | Detalle de incidente (supervisor) | Supervisor | `/incidentes/:id` | 13.4.2 | `s2-detalle-supervisor.png` (+ variante pendiente de clasificación) |
| S3 | Modal Corregir clasificación | Supervisor | diálogo en S2 | 13.4.3 | `s3-corregir-clasificacion.png` |
| S4 | Modal Asignar / Reasignar | Supervisor | diálogo en S2 | 13.4.4 | `s4-asignar.png` |
| S5 | Registrar incidente | Supervisor | `/incidentes/nuevo` | 13.4.5 | `s5-registrar-incidente.png` |
| S6 | Administración · Equipo técnico | Supervisor | `/administracion` | 13.4.6 | `s6-administracion.png` |
| S7 | Modal Alta de técnico | Supervisor | diálogo en S6 | 13.4.7 | `s7-alta-tecnico.png` |
| T1 | Mis asignados | Técnico | `/mis-asignados` | 13.5.1 | `t1-mis-asignados.png` |
| T2 | Detalle de incidente (técnico) | Técnico | `/incidentes/:id` | 13.5.2 | `t2-detalle-tecnico.png` |
| T3 | Panel de Análisis con IA | Técnico | panel dentro de T2 | 13.5.3 | `t3-analisis-ia-bloqueado.png`, `t3-analisis-ia-abierto.png`, `t3-caso-similar-abierto.png` |
| T4 | Formulario de resolución | Técnico | sección dentro de T2 | 13.5.4 | `t4-resolucion.png` |
| T5 | Modal Cerrar sin solución | Técnico | diálogo en T2 | 13.5.5 | `t5-cerrar-sin-solucion.png` |

### 13.2 Catálogo de componentes reutilizables

Se definen **una sola vez** aquí; las fichas los referencian por ID (`R01`…`R22`). Carpeta sugerida: `shared/ui/` (salvo los marcados *layout*). Todos como *standalone components* con `input()`/`output()`.

| ID | Componente | Selector sugerido | Inputs / outputs | Variantes y estados | Detalle visual / referencia |
|---|---|---|---|---|---|
| R01 | **AppShell** *(layout)* | `app-shell` | — | — | Flex: `R02 Sidebar` (238 px) + columna principal (`R03 Topbar` + `<router-outlet>`). Aloja el modal global V0. Ver 7.1 |
| R02 | **Sidebar** *(layout)* | `app-sidebar` | `currentUser`; `navigate` | Menú distinto por rol; ítem activo | Logo, sección, ítems, pie con usuario y "Cerrar sesión". Oculto <760 px. Ver 7.2–7.3 |
| R03 | **Topbar** *(layout)* | `app-topbar` | `currentUser`; `chooseUser` | Móvil: icono menú visible | Migas, notificaciones (punto rojo), ayuda, separador, selector de usuario. Ver 7.4 |
| R04 | **PageHeading** | `app-page-heading` | `eyebrow`, `title`, `subtitle`; *slot* de acciones | `detail` (alineado arriba) | Eyebrow 10 px azul mayúsculas; H1 25 px bold; subtítulo 13 px muted; acciones a la derecha. En <760 px pasa a columna y el botón ocupa el ancho |
| R05 | **StatusBadge** | `app-status-badge` | `status: IncidentStatus` | 5 colores | Tabla de colores en 7.8. 10 px semibold, radio 5 |
| R06 | **PriorityBadge** | `app-priority-badge` | `priority?: Priority` | Alta/Media/Baja; sin valor → texto "—" | Colores en 7.8 |
| R07 | **Badge** | `app-badge` | `variant: 'outline' \| 'role-supervisor' \| 'role-tech' \| 'pending' \| 'ai'` | Con icono opcional (12 px) | Pill h-20; `pending` fondo `#fff2d9` texto `#a86600`; `ai` fondo `#e9f1ff` texto `#266cd0`; `role-tech` `#e1f5f1`/`#187c72`; `role-supervisor` `#eae3ff`/`#6b43b3` |
| R08 | **UserAvatar** | `app-user-avatar` | `user?`, `large` | 32 px normal / 44 px grande | Iniciales (2 primeras letras de las iniciales del nombre); fondo `#e0edff`, texto `#1b61cf`, borde `#c6dcfd`. Sin usuario → "Administrador" |
| R09 | **StatCard** | `app-stat-card` | `label`, `value`, `helper`, `icon`, `tone: 'blue'\|'amber'\|'green'\|'red'`, `period?` | 4 tonos | Icono en cuadro 34 px; arriba a la derecha "MAYO 2024"; etiqueta 14 px muted; valor 24 px bold; ayuda 11 px muted. Ver 7.6 |
| R10 | **IncidentTable** | `app-incident-table` | `incidents`, `role`, `users`; `open(incident)` | Filtros según rol; estado vacío | Tarjeta con cabecera + filtros + tabla de 8 columnas. Ver 7.7 y fichas S1/T1 |
| R11 | **EmptyState** | `app-empty-state` | `icon`, `message` | — | Centrado, padding 64 px, icono 28 px, texto 13 px muted |
| R12 | **Card** | `app-card` (+ `card-header`, `card-content`) | `variant: 'surface' \| 'ai' \| 'success'` | Cabecera con borde inferior | `surface`: borde `#e0e6ef`, sombra `0 2px 8px #16294b08`; `ai`: borde `#cfddf4`; `success`: fondo `#f4fbf6`, borde `#c8e7d2`. Radio `xl` |
| R13 | **Button** | `button[appBtn]` | `variant`, `size`, `disabled` | `default` (primario azul), `outline`, `ghost`, `destructive`, `icon` | Alto 32 px, radio `--radius`, 14 px medium. Icono a la izquierda (16 px). Deshabilitado: opacidad 50 % |
| R14 | **FormField** (+ Input / Textarea) | `app-form-field` | `label`, `required`, `hint?`, control proyectado | Error / deshabilitado | Etiqueta 12 px semibold `#425166`; asterisco rojo `#d34040`. Input h-32; Textarea mín. 64 px |
| R15 | **Select** | `app-select` (o `mat-select`) | `options`, `placeholder`, `value` | Con icono en el trigger (filtros) | Trigger h-32 (34 px en filtros); lista con check en el valor activo |
| R16 | **Dialog** | `app-dialog` (o `MatDialog`) | `open`, `title`, `description`; *slots* contenido y pie | Anchos: `lg` (512 px), `xl` (576 px, sin padding) | Overlay; cierra con Esc y botón Cancelar. Cabecera título + descripción; pie con botones a la derecha |
| R17 | **UserOption** | `app-user-option` | `user`, `selected`, `showRole`, `large`; `select` | Seleccionado / hover | Fila con borde `#e3e8ef` radio 8; seleccionado/hover: borde `#7baaf0`, fondo `#f3f8ff`; **radio-dot** 17 px (`checked`: borde 5 px `#2a77de`) |
| R18 | **AmberNotice** | `app-notice` | `tone: 'warning'`, `icon?` | — | Fondo `amber-50`, borde `amber-200`, texto `amber-800`, 12 px, radio `lg`, padding 12–16 px |
| R19 | **SuccessCard** | (`app-card variant=success`) | `title`, `message` | — | Icono `shield-check` en círculo verde 32 px + título semibold + texto 12 px muted |
| R20 | **Separator** | `app-separator` | `orientation` | horizontal / vertical | 1 px `--border` |
| R21 | **BackLink** | `app-back-link` | `label`; `back` | — | `arrow-left` + "Volver a incidentes", 12 px semibold, azul `#326fd0` |
| R22 | **ClassificationBadges** | `app-classification-badges` | `type?`, `priority?`, `area?` | — | Tres badges: tipo (icono `tag`), prioridad (`R06`), área (icono `hard-drive`) |

**Servicios compartidos** (sección 8): `AuthService` (usuario activo), `IncidentService` (incidentes, filtros, acciones), `UserService` (técnicos), `AiService` (clasificación y recomendación, mock). **Guards:** `roleGuard(rol)` y guard de acceso por asignación.

### 13.3 Vista común · V0 — Selector de usuario activo

- **Rol / acceso:** Supervisor y Técnico. **Tipo:** modal global, abierto desde el selector de la topbar (R03). **Componente Angular:** `UserSelectorDialogComponent` (`shared/dialogs/user-selector-dialog`).
- **Objetivo:** simular el inicio de sesión: elegir con qué usuario se trabaja en la sesión. Al continuar cambia el rol y la navegación.
- **Wireframe**
  ```
  ┌────────────────────────────────────────────────┐
  │ Seleccionar usuario activo                     │
  │ Elige el usuario con el que deseas trabajar…   │
  ├────────────────────────────────────────────────┤
  │ (AG) Ana García        [Supervisor]         ◉  │
  │      Supervisor · @ana.garcia                  │
  │ (CR) Carlos Ruiz       [Técnico]            ○  │
  │      Técnico · Infraestructura · @carlos.ruiz  │
  │ … (una fila por usuario)                       │
  ├────────────────────────────────────────────────┤
  │ Selecciona un usuario para continuar. [Continuar]│
  └────────────────────────────────────────────────┘
  ```
- **Componentes que aparecen:** `R16 Dialog` (ancho `xl`, sin padding, cabecera con borde inferior `px-6 py-5`), lista de `R17 UserOption` (con `R08 UserAvatar` grande y `R07 Badge` de rol), pie con texto y `R13 Button`.
- **Reutilizables usados:** R16, R17, R08, R07, R13.
- **Específicos de esta vista:** `UserSelectorDialogComponent` (orquesta la lista, estado `selected`).
- **Datos y estado:** `users` (de `UserService`), `activeUser` (de `AuthService`); estado local `selected: string` (id), **inicializado con el usuario activo**.
- **Interacciones:** clic en una fila → cambia `selected`. **Continuar** (deshabilitado si no hay selección) → `auth.switchUser(user)` y cierra. Cerrar con Esc / clic fuera no cambia nada.
- **Reglas de negocio:** al cambiar de usuario se cierra cualquier detalle abierto y se navega a `/incidentes` (Supervisor) o `/mis-asignados` (Técnico).
- **Textos exactos:** título "Seleccionar usuario activo"; descripción "Elige el usuario con el que deseas trabajar en esta sesión."; pie "Selecciona un usuario para continuar."; botón "Continuar". Subtítulo de cada fila: `{rol}` + ` · {área}` (si tiene) y debajo `@{username}`.
- **Estados especiales:** lista vacía no contemplada (siempre hay al menos el supervisor). Los técnicos dados de alta desde S7 aparecen aquí de inmediato.
- **Responsive:** el diálogo ocupa el ancho disponible con márgenes en móvil.
- **Criterios de aceptación:** (1) abre preseleccionando el usuario activo; (2) Continuar cambia rol, menú y ruta; (3) muestra a los técnicos recién creados; (4) el badge de rol usa el color correcto.

---

### 13.4 Rol Supervisor

**Resumen del rol:** gestiona el catálogo global. **Puede:** dar de alta técnicos, registrar incidentes, ver todos los incidentes, corregir clasificación, reintentar clasificación IA, asignar/reasignar, relanzar indexación. **No puede:** analizar con IA, cambiar estado directamente, resolver/cerrar, tocar incidentes cerrados. Detalle en 1.1.
**Menú (sidebar):** sección "Gestión global" → `Incidentes` · `Registrar incidente` · `Administración`. **Migas topbar:** "Incidentes › Centro de operaciones". **Vistas:** S1–S7 (+ V0).

#### 13.4.1 S1 — Listado global de incidentes

- **Ruta / acceso:** `/incidentes` · `roleGuard('Supervisor')` (Técnico → redirige a `/mis-asignados`). Ítem activo del sidebar: "Incidentes". **Componente Angular:** `IncidentDashboardPageComponent` (`features/incidents/incident-dashboard`) que contiene `R10 IncidentTable`.
- **Objetivo:** vista de entrada del supervisor: ver de un vistazo el estado de la operación (KPIs) y localizar, filtrar y abrir cualquier incidente.
- **Wireframe**
  ```
  CENTRO DE OPERACIONES
  Incidentes                                        [+ Nuevo incidente]
  Supervisa y gestiona las incidencias de soporte de TI.
  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
  │Abiertos│ │En prog.│ │Resuelt.│ │Pend.clas│   ← 4 StatCard
  └────────┘ └────────┘ └────────┘ └────────┘
  ┌──────────────────────────────────────────────────────────────┐
  │ Todos los incidentes (6)      [🔍 Buscar…][Estado▾][Prioridad▾][⟳]│
  ├──────────────────────────────────────────────────────────────┤
  │ INCIDENTE | ESTADO | CLASIF. | PRIORIDAD | ÁREA | TÉCNICO | CREACIÓN | › │
  │ …filas clicables…                                             │
  └──────────────────────────────────────────────────────────────┘
  ```
- **Componentes que aparecen:** Shell (R01/R02/R03) · `R04 PageHeading` con botón · 4 × `R09 StatCard` · `R10 IncidentTable` (`R12 Card`, buscador, 2 × `R15 Select`, botón icono, tabla, `R05`, `R06`, `R07 pending`, `R11`).
- **Reutilizables usados:** R01–R03, R04, R05, R06, R07, R09, R10, R11, R12, R13, R15, R14 (input de búsqueda).
- **Específicos de esta vista:** `IncidentDashboardPageComponent` (compone cabecera + KPIs + tabla); fila de la tabla con icono de incidente (`clipboard-list` o `alert-circle` ámbar si `pendingClassification`); buscador con icono `search`.
- **Datos y estado**
  - Fuente: `IncidentService.visible()` (todos los incidentes para el supervisor) y `UserService.users()` (para resolver el nombre del técnico por `username`).
  - Filtros locales (signals): `search`, `status` (`'all'` por defecto), `priority` (`'all'`). `filtered = computed(...)` (código en sección 8).
  - **KPIs** (mockup = valores fijos; en Angular calcular con `computed`):
    | Etiqueta | Valor mockup | Ayuda | Icono | Tono | Cálculo sugerido |
    |---|---|---|---|---|---|
    | Incidentes abiertos | 18 | +3 esta semana | clipboard-list | blue | estado ≠ Resuelto/Cerrado |
    | En progreso | 07 | 2 de prioridad alta | activity | amber | estado = En progreso |
    | Resueltos este mes | 42 | +12% vs. mes anterior | shield-check | green | Resuelto en el mes actual |
    | Pendientes de clasificación | 03 | Requieren revisión | alert-circle | red | `pendingClassification` |
    Formato de valor: 2 dígitos (`padStart(2,'0')`). Período fijo "MAYO 2024" en el mockup.
- **Interacciones**
  - Clic en **fila** → `/incidentes/:id` (S2).
  - **+ Nuevo incidente** → `/incidentes/nuevo` (S5).
  - Buscador: filtra por `title` o `id`, sin distinguir mayúsculas.
  - Select **Estado** (solo supervisor): "Todos los estados" + Registrado, Asignado, En progreso, Resuelto, Cerrado sin solución.
  - Select **Prioridad**: "Todas las prioridades" + Alta, Media, Baja. Los incidentes **sin prioridad** quedan excluidos al filtrar por una prioridad concreta.
  - Botón icono `refresh-cw` (aria-label "Limpiar filtros") → restablece búsqueda, estado y prioridad.
- **Reglas de negocio:** el supervisor ve **todos** los incidentes. Ordenar por fecha de creación descendente (el mockup ya los trae en ese orden). Localizar pendientes de clasificación mediante el badge "Pendiente".
- **Columnas de la tabla (8)**
  | Columna | Contenido |
  |---|---|
  | Incidente | Icono 28 px + título (12 px semibold, truncado a 300 px) + ID monoespaciado 10 px muted |
  | Estado | `R05 StatusBadge` |
  | Clasificación | Si pendiente → `R07 pending` "Pendiente"; si no → `type` (12 px muted) |
  | Prioridad | `R06 PriorityBadge` (o "—") |
  | Área responsable | `area` o "Sin asignar" |
  | Técnico asignado | Nombre completo o "—" |
  | Creación | Solo la fecha (`dd/MM/yyyy`) |
  | (sin título) | Chevron `chevron-right` |
- **Textos exactos:** eyebrow "Centro de operaciones"; H1 "Incidentes"; subtítulo "Supervisa y gestiona las incidencias de soporte de TI."; título de tarjeta "Todos los incidentes" + contador (pill gris con nº de incidentes visibles); subtítulo de tarjeta "Consulta y gestiona el catálogo global de incidencias."; placeholder "Buscar por título o ID..."; botón "Nuevo incidente".
- **Estados especiales:** vacío (`R11`): icono `search` + "No hay incidentes que coincidan con los filtros."
- **Responsive:** ≤1100 px KPIs a 2 columnas y paddings menores; ≤760 px cabecera en columna con botón a ancho completo, buscador 100 %, selects `flex:1`; la tabla mantiene ancho mínimo 1000 px con scroll horizontal; ≤430 px KPIs a 1 columna.
- **Criterios de aceptación:** (1) 6 filas con los datos semilla; (2) cada filtro reduce la lista y "Limpiar" la restaura; (3) `INC-2024-0407` muestra icono ámbar y badge "Pendiente"; (4) clic en fila abre S2; (5) un técnico que fuerce la URL es redirigido; (6) KPIs coinciden con los datos.
- **Notas:** el mockup **no ordena** ni pagina; definir paginación si el volumen crece. Las filas deberían ser operables con teclado (ver sección 12).

#### 13.4.2 S2 — Detalle de incidente (supervisor)

- **Ruta / acceso:** `/incidentes/:id` · Supervisor (cualquier incidente). Ítem del sidebar activo: "Incidentes". **Componente Angular:** `IncidentDetailPageComponent` (`features/incidents/incident-detail`), mostrando la variante de supervisor.
- **Objetivo:** consultar toda la información de un incidente y ejecutar las acciones de gestión: reintentar/corregir clasificación, asignar/reasignar y relanzar indexación. **Modo consulta:** no puede cambiar el estado ni usar la IA de diagnóstico.
- **Wireframe**
  ```
  ← Volver a incidentes
  INC-2024-0407 [Registrado]
  Nueva solicitud sin clasificación
  Creado el 20/05/2024 10:14 por Ana García · Sin técnico asignado
                         [⟳ Reintentar clasificación IA][✎ Corregir clasificación][👤 Asignar]
  ┌─────────────────────────────────────┐ ┌───────────────────────────┐
  │ Detalle del incidente               │ │ Acciones del supervisor   │
  │ DESCRIPCIÓN … texto …               │ │ Puedes corregir la        │
  │ ─────────────                       │ │ clasificación, asignar…   │
  │ CLASIFICACIÓN [aviso ámbar | badges]│ └───────────────────────────┘
  │ fuente de clasificación             │
  └─────────────────────────────────────┘
  [⟳ Relanzar indexación]   (solo Resuelto + sin indexar)
  ```
  Rejilla 2 columnas (`1.1fr / .9fr`); 1 columna en <760 px.
- **Componentes que aparecen:** `R21 BackLink` · `R04 PageHeading` (variante detalle) con id monoespaciado + `R05` + H1 + subtítulo + grupo de `R13 Button` · `R12 Card` "Detalle del incidente" (bloque descripción, `R20`, bloque clasificación con `R22` o `R18`) · `R12 Card` "Acciones del supervisor" · botón "Relanzar indexación" · diálogos S3 y S4.
- **Reutilizables usados:** R01–R03, R21, R04, R05, R06, R12, R13, R18, R20, R22, R16 (vía S3/S4).
- **Específicos de esta vista:** `IncidentDetailPageComponent`; bloque de descripción/clasificación (`IncidentSummaryComponent`, reutilizable también por T2); tarjeta informativa "Acciones del supervisor".
- **Datos y estado**
  - `incident = computed(() => incidents.byId(routeId))`; `assigned = computed(() => users.find(username === incident.assignedTo))`.
  - Estado local: `retrying` (signal, ver 7.15), `assignOpen`, `classificationOpen`.
  - `isFinal = status ∈ {Resuelto, Cerrado sin solución}`; `canSupervisorEdit = !isFinal`.
- **Botones (solo si `canSupervisorEdit`), de izquierda a derecha**
  | Botón | Variante / icono | Condición | Acción |
  |---|---|---|---|
  | Reintentar clasificación IA | outline · `refresh-cw` | `pendingClassification` | `retryClassification()` (7.15). Cargando: "Clasificando con IA..." + icono girando + deshabilitado |
  | Corregir clasificación | outline · `pencil` | siempre (no cerrado) | abre S3 |
  | Asignar / Reasignar | primario · `user-check` | Asignar si sin técnico y con área; Reasignar si Asignado o En progreso | abre S4. **Ajuste respecto al mockup:** ocultar/deshabilitar Asignar si `pendingClassification` (tooltip "Clasifica el incidente antes de asignar") |
  | Relanzar indexación | outline · `refresh-cw` | `status = Resuelto` y `unindexed` | `reindex(id)` → `unindexed = false`. Se ubica **bajo** la tarjeta de detalle |
- **Reglas de negocio:** estados finales → sin botones de edición. Reasignar solo en Asignado/En progreso y a técnico del área responsable. Corregir o reintentar clasificación no cambia el estado. Asignar → `status = 'Asignado'`.
- **Textos exactos:** enlace "Volver a incidentes"; subtítulo `Creado el {createdAt} por {createdBy fullName} · {Asignado a {fullName} | Sin técnico asignado}`; tarjeta "Detalle del incidente"; etiquetas "Descripción" y "Clasificación"; fuente `incident.classificationSource` o "Pendiente de clasificación"; aviso ámbar "Pendiente de clasificación. El supervisor debe completarla antes de asignar."; tarjeta "Acciones del supervisor": "Puedes corregir la clasificación, asignar al técnico del área responsable y relanzar la indexación. El estado cambia automáticamente por las acciones del técnico."
- **Estados especiales / variantes**
  | Incidente de prueba | Qué se ve |
  |---|---|
  | `INC-2024-0407` (pendiente) | Aviso ámbar; botones Reintentar + Corregir + Asignar |
  | `INC-2024-0410` (Registrado, clasificado) | Badges de clasificación; Corregir + Asignar |
  | `INC-2024-0411` (Asignado, María López) | Corregir + Reasignar |
  | `INC-2024-0412` (En progreso, Carlos Ruiz) | Corregir + Reasignar |
  | `INC-2024-0409` (Resuelto, sin indexar) | Sin botones de edición; botón "Relanzar indexación" |
  | `INC-2024-0408` (Cerrado sin solución) | Solo lectura |
- **Responsive:** ≤760 px una columna; los botones de acción ocupan el ancho (`flex:1`, `flex-wrap`).
- **Criterios de aceptación:** (1) el botón Reintentar solo aparece en incidentes pendientes y tras el éxito desaparece y se muestran los badges; (2) Asignar abre S4 con técnicos del área; (3) un incidente cerrado no muestra acciones; (4) volver regresa al listado conservando el rol; (5) el estado de la cabecera refleja el estado real.
- **Notas / mejoras recomendadas (no están en el mockup):** mostrar el bloque **"Solución"** (`incident.solution`, `resolvedAt`) en incidentes Resuelto y el **"Motivo de cierre"** (`closureReason`, `closedAt`) en Cerrado sin solución; mostrar línea de tiempo (creado / asignado / iniciado / resuelto) con `createdAt`, `assignedAt`, `startedAt`, `resolvedAt`.

#### 13.4.3 S3 — Modal Corregir clasificación

- **Ruta / acceso:** diálogo abierto desde S2. Solo Supervisor, incidente no cerrado. **Componente:** `ClassificationDialogComponent` (`features/incidents/classification-dialog`).
- **Objetivo:** que el supervisor corrija manualmente tipo, prioridad y área responsable (por ejemplo cuando la IA se equivoca o quedó pendiente).
- **Wireframe**
  ```
  Corregir clasificación
  Actualiza los datos antes de asignar el incidente.
  Tipo               [ Bug            ▾]
  Prioridad          [ Alta           ▾]
  Área responsable   [ Infraestructura▾]
                                  [Cancelar] [Guardar clasificación]
  ```
- **Componentes que aparecen:** `R16 Dialog` (ancho `lg`) · 3 × `R14 FormField` + `R15 Select` · 2 × `R13 Button`.
- **Reutilizables usados:** R16, R14, R15, R13.
- **Específicos:** `ClassificationDialogComponent` con formulario reactivo `{ type, priority, area }`.
- **Datos y estado:** entrada `incident`; valores iniciales = los actuales (vacíos si está pendiente). Catálogos: tipos `Bug, Rendimiento, Acceso, Hardware, Red, Configuración`; prioridades `Alta, Media, Baja`; áreas `Infraestructura, Aplicaciones, Base de datos, Seguridad, Soporte a usuario`.
- **Interacciones:** Cancelar → cierra sin cambios. Guardar → `incidents.correctClassification(id, {type, priority, area})`.
- **Reglas de negocio:** al guardar: `classificationSource = 'Corregida manualmente'`, `pendingClassification = false`; el estado no cambia. Los tres campos son obligatorios (**el mockup no lo valida ni persiste**: implementarlo). Si se cambia el área de un incidente ya asignado a un técnico de otra área, avisar/exigir reasignar (decisión abierta).
- **Textos exactos:** título "Corregir clasificación"; descripción "Actualiza los datos antes de asignar el incidente."; etiquetas "Tipo", "Prioridad", "Área responsable"; botones "Cancelar" y "Guardar clasificación".
- **Criterios de aceptación:** (1) precarga los valores actuales; (2) Guardar deshabilitado si falta algún campo; (3) tras guardar, S2 y S1 reflejan el cambio y desaparece "Pendiente"; (4) Esc cierra.

#### 13.4.4 S4 — Modal Asignar / Reasignar

- **Ruta / acceso:** diálogo abierto desde S2. Solo Supervisor. **Componente:** `AssignDialogComponent` (`features/incidents/assign-dialog`).
- **Objetivo:** asignar el incidente a un técnico del área responsable, o cambiar el técnico (reasignar).
- **Wireframe**
  ```
  Asignar incidente                (o "Reasignar incidente")
  Solo se muestran técnicos del área responsable: Infraestructura.
  (CR) Carlos Ruiz     carlos.ruiz@empresa.es      ◉
  (PS) Pedro Sánchez   pedro.sanchez@empresa.es    ○
                                        [Cancelar] [Asignar]
  ```
- **Componentes que aparecen:** `R16 Dialog` (`lg`) · lista de `R17 UserOption` (con `R08`) · `R18 AmberNotice` si no hay candidatos · 2 × `R13 Button`.
- **Reutilizables usados:** R16, R17, R08, R18, R13.
- **Específicos:** `AssignDialogComponent` (calcula candidatos y selección).
- **Datos y estado:** entrada `incident`; `candidates = users.filter(role==='Técnico' && area===incident.area)`; `selected` (username) precargado con `incident.assignedTo`.
- **Interacciones:** clic en un técnico → selecciona. **Asignar** (deshabilitado sin selección) → `incidents.assign(id, username)` y cierra. Cancelar cierra.
- **Reglas de negocio:** solo técnicos **del área responsable**; Reasignar únicamente con estado Asignado o En progreso; no se puede asignar un incidente sin área (pendiente de clasificación). Al asignar → `status = 'Asignado'` y `assignedTo`. El técnico anterior pierde acceso al incidente. (Recomendado: rellenar `assignedAt`.)
- **Textos exactos:** título "Asignar incidente" / "Reasignar incidente" (según exista `assignedTo`); descripción `Solo se muestran técnicos del área responsable: {area | "sin área"}.`; aviso "No hay técnicos dados de alta en el área responsable."; botones "Cancelar" y "Asignar". Subtexto de cada técnico: `{username}@empresa.es`.
- **Criterios de aceptación:** (1) `INC-2024-0412` (Infraestructura) lista a Carlos y Pedro; `INC-2024-0411` (Aplicaciones) solo a María; (2) sin candidatos → aviso ámbar y botón deshabilitado; (3) tras asignar, el incidente pasa a "Asignado" y aparece en el listado del técnico.
- **Nota:** el mockup en Reasignar mantiene el estado en "Asignado" incluso si estaba En progreso (el estado se fuerza a Asignado); confirmar si al reasignar un incidente En progreso debe volver a Asignado (lo lógico, porque el nuevo técnico debe iniciarlo).

#### 13.4.5 S5 — Registrar incidente

- **Ruta / acceso:** `/incidentes/nuevo` · `roleGuard('Supervisor')`. Ítem activo: "Registrar incidente". **Componente:** `IncidentRegisterPageComponent` (`features/incidents/incident-register`).
- **Objetivo:** dar de alta un incidente con título y descripción; la IA clasifica automáticamente tipo, prioridad y área.
- **Wireframe**
  ```
  NUEVO REGISTRO
  Registrar incidente
  Describe el problema para que podamos ayudarte.
  ┌──────────────────────────────────────────────┐
  │ Detalles del incidente                       │
  │ La IA clasificará automáticamente el tipo…   │
  ├──────────────────────────────────────────────┤
  │ Título *        [ Ej.: Error 500 al …     ]  │
  │ Descripción *   [ textarea (mín. 144 px)  ]  │
  │ ⚠ No incluyas contraseñas, tokens ni info…   │
  │ [✨ Registrar y clasificar con IA] (ancho 100%)│
  └──────────────────────────────────────────────┘
  ( tarjeta verde de éxito tras enviar )
  ```
  Contenedor estrecho (`max-width: 1000px`).
- **Componentes que aparecen:** `R04 PageHeading` · `R12 Card` con cabecera y contenido · 2 × `R14 FormField` (Input y Textarea) · aviso con icono `alert-triangle` ámbar · `R13 Button` a ancho completo con icono `sparkles` · `R19 SuccessCard` (tras enviar).
- **Reutilizables usados:** R04, R12, R14, R13, R19, R18 (estilo del aviso).
- **Específicos:** `IncidentRegisterPageComponent` (formulario reactivo `{ title, description }`).
- **Datos y estado:** formulario reactivo con `Validators.required` en ambos campos (sugerido `minLength`); signal `submitted` / `created` (incidente resultante); estado de carga durante la clasificación.
- **Interacciones:** **Registrar y clasificar con IA** → `incidents.create({title, description})` (crea con estado `Registrado`, `createdBy` = usuario activo, ID correlativo) → `ai.classify()` → si tiene éxito `classificationSource = 'Clasificado por IA'`; si falla queda `pendingClassification = true` (luego S2 permite reintentar). Muestra la tarjeta de éxito.
- **Reglas de negocio:** solo Supervisor registra. No incluir contraseñas ni datos sensibles (aviso, no validación técnica). Un incidente sin clasificar queda como "Pendiente de clasificación".
- **Textos exactos:** eyebrow "Nuevo registro"; H1 "Registrar incidente"; subtítulo "Describe el problema para que podamos ayudarte."; tarjeta "Detalles del incidente"; ayuda "La IA clasificará automáticamente el tipo, prioridad y área responsable."; etiquetas "Título" y "Descripción" (con `*`); placeholders "Ej.: Error 500 al intentar generar reporte de ventas" y "Explica el problema con el mayor detalle posible. No escribas contraseñas ni datos sensibles."; aviso "No incluyas contraseñas, tokens ni información sensible."; botón "Registrar y clasificar con IA"; éxito: **"Incidente registrado correctamente"** / "Se ha creado el incidente {id} y fue clasificado por IA."
- **Estados especiales:** cargando (botón deshabilitado + "Clasificando…"), error de clasificación (crear igualmente y avisar "Incidente registrado, pendiente de clasificación"), éxito.
- **Criterios de aceptación:** (1) sin título o descripción el botón no envía y marca los campos; (2) al enviar aparece la tarjeta verde con el ID real; (3) el incidente aparece en S1; (4) limpia el formulario tras el éxito.
- **Nota:** el mockup **no crea** el incidente ni valida; el ID `INC-2024-0415` está fijo.

#### 13.4.6 S6 — Administración · Equipo técnico

- **Ruta / acceso:** `/administracion` · `roleGuard('Supervisor')`. Ítem activo: "Administración". **Componente:** `TeamPageComponent` (`features/admin/team-page`).
- **Objetivo:** ver los técnicos registrados y darlos de alta para que puedan recibir incidentes de su área.
- **Wireframe**
  ```
  ADMINISTRACIÓN
  Equipo técnico                                   [+ Dar de alta técnico]
  Gestiona los técnicos disponibles para asignar incidentes.
  ┌──────────────────────────────────────────────┐
  │ Técnicos registrados (3)                     │
  ├──────────────────────────────────────────────┤
  │ (CR) Carlos Ruiz                             │
  │      carlos.ruiz@empresa.es · Infraestructura   [Técnico] [✎] │
  │ … una fila por técnico                       │
  └──────────────────────────────────────────────┘
  ```
- **Componentes que aparecen:** `R04 PageHeading` con botón · `R12 Card` (cabecera con contador) · filas `team-row` (`R08` grande + textos + `R07 role-tech` + botón icono `pencil`) · diálogo S7.
- **Reutilizables usados:** R04, R12, R08, R07, R13, R16 (S7).
- **Específicos:** `TeamPageComponent`; fila `TeamRowComponent` (avatar, nombre, `username@empresa.es · área`, badge, botón editar).
- **Datos y estado:** `users.filter(role === 'Técnico')` (el supervisor no se lista); signal `createOpen`.
- **Interacciones:** "Dar de alta técnico" → abre S7. Icono lápiz (aria-label `Editar {nombre}`) → **sin acción en el mockup** (definir edición: nombre, área, baja).
- **Reglas de negocio:** solo Supervisor. Un técnico recién creado puede recibir asignaciones de incidentes de su área y aparece en V0.
- **Textos exactos:** eyebrow "Administración"; H1 "Equipo técnico"; subtítulo "Gestiona los técnicos disponibles para asignar incidentes."; botón "Dar de alta técnico"; tarjeta "Técnicos registrados" + contador; badge "Técnico".
- **Estados especiales:** lista vacía (no contemplada en el mockup): mostrar `R11` "Aún no hay técnicos registrados".
- **Criterios de aceptación:** (1) lista 3 técnicos semilla con su área; (2) el contador coincide; (3) tras crear uno aparece de inmediato y sube el contador; (4) contenedor estrecho centrado.

#### 13.4.7 S7 — Modal Alta de técnico

- **Ruta / acceso:** diálogo abierto desde S6. Solo Supervisor. **Componente:** `UserCreateDialogComponent` (`features/admin/user-create-dialog`).
- **Objetivo:** registrar un técnico con su área para que pueda recibir incidentes.
- **Wireframe**
  ```
  Dar de alta técnico
  Registra un técnico para que pueda recibir incidentes de su área.
  Nombre completo *      [ Ej.: Laura Martínez ]
  Usuario corporativo *  [ laura.martinez      ]
  Área técnica *         [ Selecciona un área ▾]
                                [Cancelar] [Crear técnico]
  ```
- **Componentes que aparecen:** `R16 Dialog` (`lg`) · 3 × `R14 FormField` (2 Input + 1 `R15 Select`) · 2 × `R13 Button`.
- **Reutilizables usados:** R16, R14, R15, R13.
- **Específicos:** `UserCreateDialogComponent` (formulario reactivo `{ fullName, username, area }`).
- **Datos y estado:** áreas = las 5 de `AREAS`. Al crear: `{ id: 'u' + Date.now(), fullName, username, role: 'Técnico', area }` → `users.add()`; limpia el formulario y cierra.
- **Interacciones:** **Crear técnico** deshabilitado hasta completar los tres campos. Cancelar cierra.
- **Reglas de negocio:** `role` siempre `Técnico`; **validar unicidad de `username`** (sugerido, no está en el mockup) y formato `nombre.apellido`.
- **Textos exactos:** título "Dar de alta técnico"; descripción "Registra un técnico para que pueda recibir incidentes de su área."; etiquetas "Nombre completo", "Usuario corporativo", "Área técnica" (con `*`); placeholders "Ej.: Laura Martínez", "laura.martinez", "Selecciona un área"; botones "Cancelar" y "Crear técnico".
- **Criterios de aceptación:** (1) botón deshabilitado con campos vacíos; (2) tras crear, el técnico aparece en S6, en V0 y como candidato en S4 (si el área coincide); (3) el formulario queda limpio al reabrir.

---

### 13.5 Rol Técnico

**Resumen del rol:** trabaja únicamente sobre sus incidentes. **Puede:** ver solo sus asignados, iniciar, analizar con IA, resolver (≥30 caracteres, solo En progreso), cerrar sin solución con motivo. Es el **único** rol que cambia el estado. **No puede:** registrar, dar de alta técnicos, corregir clasificación, asignar/reasignar, reindexar, ver incidentes ajenos ni actuar tras ser reasignado. Detalle en 1.1.
**Menú (sidebar):** sección "Mi trabajo" → `Mis asignados` (único ítem). **Migas topbar:** "Mis asignados › Centro de operaciones". **Vistas:** T1–T5 (+ V0).

**Datos de prueba por técnico (semilla):**
| Usuario | Área | Incidentes asignados |
|---|---|---|
| Carlos Ruiz | Infraestructura | `INC-2024-0412` (En progreso) |
| María López | Aplicaciones | `INC-2024-0411` (Asignado), `INC-2024-0409` (Resuelto, sin indexar) |
| Pedro Sánchez | Infraestructura | ninguno (sirve para probar el estado vacío) |

#### 13.5.1 T1 — Mis asignados

- **Ruta / acceso:** `/mis-asignados` · `roleGuard('Técnico')` (Supervisor → redirige a `/incidentes`). Ítem activo: "Mis asignados". **Componente:** `IncidentDashboardPageComponent` en modo técnico (mismo componente que S1, distinto contenido según rol).
- **Objetivo:** ver rápidamente el trabajo propio (KPIs personales) y abrir sus incidentes asignados.
- **Wireframe**
  ```
  MI ESPACIO DE TRABAJO
  Mis asignados
  Incidentes asignados a Carlos Ruiz.
  ┌────────┐ ┌────────┐ ┌────────┐
  │Asign.  │ │En prog.│ │Resuelt.│      ← 3 StatCard
  └────────┘ └────────┘ └────────┘
  ┌──────────────────────────────────────────────────────────────┐
  │ Mis incidentes asignados (1)        [🔍 Buscar…][Prioridad▾][⟳] │
  │ Solo puedes ver incidentes donde eres el técnico responsable.│
  │ tabla (8 columnas, mismas que S1)                            │
  └──────────────────────────────────────────────────────────────┘
  ```
- **Componentes que aparecen:** `R04 PageHeading` (sin botón de acción) · 3 × `R09 StatCard` · `R10 IncidentTable` (sin filtro de estado).
- **Reutilizables usados:** R01–R03, R04, R05, R06, R07, R09, R10, R11, R12, R15, R14.
- **Específicos:** ninguno propio; se reutiliza S1 con `role = 'Técnico'`.
- **Datos y estado:** `IncidentService.visible()` filtra por `assignedTo === currentUser.username`. Filtros locales: `search` y `priority`. **KPIs:**
  | Etiqueta | Valor | Ayuda | Icono | Tono |
  |---|---|---|---|---|
  | Asignados a mí | nº de incidentes visibles (`padStart(2,'0')`) | Requieren atención | user-check | blue |
  | En progreso | nº con estado En progreso | Trabajo activo | activity | amber |
  | Resueltos este mes | 12 (fijo en el mockup; calcular) | Buen ritmo | shield-check | green |
  Con 3 tarjetas el grid de 4 columnas deja un hueco a la derecha (comportamiento del mockup).
- **Interacciones:** clic en fila → `/incidentes/:id` (T2). Buscador y prioridad como en S1. Limpiar filtros (icono `refresh-cw`).
- **Reglas de negocio:** **solo** incidentes asignados a él (de su área). No hay botón "Nuevo incidente" ni filtro por estado.
- **Textos exactos:** eyebrow "Mi espacio de trabajo"; H1 "Mis asignados"; subtítulo "Incidentes asignados a {fullName}."; tarjeta "Mis incidentes asignados" + contador; subtítulo "Solo puedes ver incidentes donde eres el técnico responsable."; vacío "No hay incidentes que coincidan con los filtros."
- **Estados especiales:** Pedro Sánchez → KPIs `00`, tabla vacía con `R11`. Un incidente reasignado a otro técnico desaparece del listado.
- **Responsive:** igual que S1.
- **Criterios de aceptación:** (1) Carlos ve 1 fila, María 2, Pedro 0; (2) sin filtro de estado; (3) sin botón de nuevo incidente; (4) los KPIs se recalculan con los datos.

#### 13.5.2 T2 — Detalle de incidente (técnico)

- **Ruta / acceso:** `/incidentes/:id` · Técnico **solo si** `incident.assignedTo === username` (guard por asignación; si no, redirigir a T1). **Componente:** `IncidentDetailPageComponent` (variante técnico).
- **Objetivo:** trabajar el incidente: iniciarlo, pedir apoyo diagnóstico a la IA, resolverlo o cerrarlo sin solución.
- **Wireframe**
  ```
  ← Volver a incidentes
  INC-2024-0412 [En progreso]
  Fallo de conectividad en servidor de base de datos
  Creado el 22/05/2024 08:17 por Ana García · Asignado a Carlos Ruiz
      [⚡ Iniciar trabajo][✨ Analizar con IA][🛡 Resolver][Cerrar sin solución]
  ┌───────────────────────────────┐ ┌───────────────────────────────┐
  │ Detalle del incidente         │ │ 🤖 Recomendación de diagnóstico │
  │ Descripción / Clasificación   │ │ Asistencia para tu análisis… [IA]│
  │ (tarjeta verde al resolver)   │ │ (bloqueado | pasos + casos)     │
  └───────────────────────────────┘ └───────────────────────────────┘
  ```
- **Componentes que aparecen:** `R21 BackLink` · `R04 PageHeading` (detalle) + `R05` + botones · `R12 Card` "Detalle del incidente" (con `R22`, `R20`, y la sección T4 al resolver) · **Panel de IA** (T3) · diálogo T5.
- **Reutilizables usados:** R01–R03, R21, R04, R05, R06, R12, R13, R20, R22, R18, R16 (T5).
- **Específicos:** `IncidentDetailPageComponent`; `IncidentSummaryComponent` (compartido con S2); `AiRecommendationPanelComponent` (T3); `ResolutionFormComponent` (T4).
- **Datos y estado:** `incident`, `assigned`, `isTech = role==='Técnico' && incident.assignedTo===username`. Estado efectivo mostrado en la cabecera: `Resuelto` si se acaba de resolver; `En progreso` si se inició estando Asignado; si no, el estado real. Locales: `workStarted` (inicial `status === 'En progreso'`), `resolved`, `aiOpen`, `closeOpen`. **En Angular estos cambios deben persistirse en `IncidentService`** (en el mockup se pierden al salir).
- **Botones (solo si `isTech` y estado no final), de izquierda a derecha**
  | Botón | Variante / icono | Visible cuando | Acción |
  |---|---|---|---|
  | Iniciar trabajo | primario · `zap` | estado Asignado (no iniciado) | `start(id)` → En progreso (+ `startedAt`) |
  | Analizar con IA | outline · `sparkles` | Asignado o En progreso | abre el panel T3 (`aiOpen = true`) y lanza `ai.recommend()` |
  | Resolver | primario · `shield-check` | **solo En progreso** (tras iniciar) | muestra el formulario T4 |
  | Cerrar sin solución | ghost | Asignado o En progreso | abre T5 |
- **Reglas de negocio:** ver matriz rol × estado (1.1.C). Resolver exige haber iniciado. Tras Resolver o Cerrar el incidente queda final y desaparecen los botones. Si el supervisor lo reasigna, el técnico pierde acceso.
- **Textos exactos:** los mismos de la cabecera de S2 (id, badge, título, subtítulo `Creado el … por … · Asignado a …`); tarjeta "Detalle del incidente"; etiquetas "Descripción" y "Clasificación"; botones "Iniciar trabajo", "Analizar con IA", "Resolver", "Cerrar sin solución". En la tarjeta de detalle **no** hay botón de reindexación (solo supervisor).
- **Estados especiales / variantes**
  | Incidente | Vista |
  |---|---|
  | `INC-2024-0411` (Asignado, María) | Iniciar trabajo · Analizar con IA · Cerrar sin solución (sin Resolver) |
  | `INC-2024-0412` (En progreso, Carlos) | Analizar con IA · Resolver · Cerrar sin solución |
  | `INC-2024-0409` (Resuelto, María) | Sin botones; solo lectura (recomendado mostrar la solución) |
- **Responsive:** ≤760 px una columna; botones a ancho completo con `flex-wrap`.
- **Criterios de aceptación:** (1) un técnico no accede a incidentes ajenos (ni por URL); (2) Iniciar cambia a "En progreso" y muestra Resolver; (3) los botones respetan la matriz de estados; (4) tras resolver/cerrar no quedan acciones; (5) el panel de IA solo aparece para el técnico asignado.

#### 13.5.3 T3 — Panel de Análisis con IA (dentro de T2)

- **Ruta / acceso:** columna derecha de T2. Solo técnico asignado; incidente Asignado o En progreso. **Componente:** `AiRecommendationPanelComponent` (`features/incidents/ai-recommendation-panel`). Detalle ampliado en 7.11 y 7.16.
- **Objetivo:** ofrecer una recomendación de diagnóstico (pasos) y casos similares ya resueltos como apoyo. **La decisión final es del técnico.**
- **Wireframe**
  ```
  Estado bloqueado                 Estado abierto
  ┌───────────────────────┐        ┌───────────────────────────────────┐
  │ 🤖 Recomendación…  [IA]│        │ 🤖 Recomendación…  [IA]           │
  │        ✨              │        │ Basado en análisis de patrones…    │
  │ Pulsa “Analizar con IA”│        │ ① Verificar estado del servicio…   │
  │ para obtener pasos…    │        │ ② Comprobar conectividad y puertos │
  └───────────────────────┘        │ ③ Revisar logs de aplicación…      │
                                   │ ─────────────                      │
                                   │ Casos similares usados (3)         │
                                   │ ▸ Fallo de acceso a APP-SRV-01 92% │
                                   │ ▾ Caída del servicio de BD… 84%    │
                                   │   [detalle desplegado]             │
                                   │ ℹ Esta es una sugerencia…          │
                                   └───────────────────────────────────┘
  ```
- **Componentes que aparecen:** `R12 Card variant=ai` · cabecera (icono `bot` en cuadro azul 31 px, título, subtítulo, `R07 ai` "IA") · estado bloqueado (icono `sparkles` + texto) · lista de pasos numerados · `R20` · lista de casos similares (`SimilarCaseItemComponent`) · callout azul.
- **Reutilizables usados:** R12, R07, R20, R05, R06, R22 (en el detalle del caso), R18 (estilo).
- **Específicos de esta vista:** `AiRecommendationPanelComponent`, `AiStepComponent` (círculo azul con número + título + descripción), `SimilarCaseItemComponent` (fila-botón + detalle desplegable), callout informativo.
- **Datos y estado:** `AIRecommendation` (sección 3) `{ steps, referenceIncidents: SimilarCase[], loading, error }` obtenido de `ai.recommend(incident)`. Signals: `openCase: string | null`. Mockup: pasos fijos y casos de `SIMILAR_CASES_KB` (sección 4).
- **Interacciones:** botón "Analizar con IA" (en T2) desbloquea el panel. Clic en un caso similar → despliega su detalle **en el mismo panel** (acordeón, uno a la vez, `aria-expanded`); otro clic lo cierra. Sin navegación.
- **Contenido del estado abierto**
  - Intro: "Basado en análisis de patrones y casos similares:".
  - Pasos (3, mockup): "Verificar estado del servicio y del servidor", "Comprobar conectividad y puertos", "Revisar logs de aplicación y balanceador"; cada uno con la descripción "Valida la conectividad, revisa recursos y contrasta el comportamiento reciente."
  - "Casos similares usados (N) · pulsa un caso para ver el detalle".
  - Cada caso: icono `clipboard-list`, título, ID, pill de similitud (`92%`), chevron. Detalle: badges (estado, tipo, prioridad, área), **Descripción**, **Solución aplicada**, "Resuelto por {nombre} · {fecha}".
  - Callout: "Esta es una sugerencia. La decisión final es del técnico."
- **Reglas de negocio:** panel solo para el técnico asignado (el supervisor ve en su lugar la tarjeta "Acciones del supervisor"). Los casos similares son de solo lectura.
- **Textos exactos:** título "Recomendación de diagnóstico"; subtítulo "Asistencia para tu análisis técnico"; bloqueado: "Pulsa “Analizar con IA” para obtener pasos de diagnóstico y casos similares."
- **Estados especiales:** bloqueado · cargando (`loading`, esqueleto/spinner) · error (mensaje y "Reintentar") · sin casos ("No se encontraron casos similares") · abierto.
- **Criterios de aceptación:** (1) arranca bloqueado; (2) "Analizar con IA" lo abre; (3) 3 casos, los del área del incidente primero; (4) el detalle se despliega en el sitio y solo uno a la vez; (5) el callout final siempre visible.

#### 13.5.4 T4 — Formulario de resolución (dentro de T2)

- **Ruta / acceso:** sección dentro de la tarjeta "Detalle del incidente" de T2. Solo técnico asignado con incidente **En progreso**. **Componente:** `ResolutionFormComponent`.
- **Objetivo:** registrar la solución aplicada y cerrar el incidente como **Resuelto**.
- **Wireframe**
  ```
  ┌ tarjeta verde ───────────────────────────────────────┐
  │ Incidente resuelto                                   │
  │ La solución requiere al menos 30 caracteres y        │
  │ quedará registrada en el historial.                  │
  │ [ textarea (mín. 96 px)                            ] │
  │ 12/30                            [Confirmar resolución] │
  └──────────────────────────────────────────────────────┘
  ```
- **Componentes que aparecen:** `R12`/`R19` (variante success, fondo `#f4fbf6`) · `R14` Textarea · contador · `R13 Button`.
- **Reutilizables usados:** R12/R19, R14, R13.
- **Específicos:** `ResolutionFormComponent` (control `solution` con `Validators.minLength(30)`).
- **Datos y estado:** `solution: string`; estado `resolved` (mockup: se activa al pulsar "Resolver").
- **Interacciones:** pulsar **Resolver** en T2 muestra el formulario; **Confirmar resolución** (deshabilitado con menos de 30 caracteres) → `incidents.resolve(id, solution)` → `Resuelto`, `solution` y `resolvedAt`. (Qué proceso marca el incidente como `unindexed` y lo indexa en la base de conocimiento de la IA está por definir; ver sección 12.)
- **Reglas de negocio:** mínimo **30 caracteres**; solo desde En progreso; resolver es definitivo (no se puede reabrir).
- **Textos exactos:** "Incidente resuelto" y "La solución requiere al menos 30 caracteres y quedará registrada en el historial." El mockup precarga el textarea con "Se validó el servicio, se corrigió la configuración afectada y se comprobó el funcionamiento."
- **Nota:** **el mockup no tiene botón de confirmar, contador ni validación** y no persiste; son requisitos a implementar según la especificación.
- **Criterios de aceptación:** (1) no se puede confirmar con <30 caracteres; (2) al confirmar el estado pasa a Resuelto y desaparecen los botones; (3) la solución queda guardada en el incidente.

#### 13.5.5 T5 — Modal Cerrar sin solución

- **Ruta / acceso:** diálogo abierto desde T2. Solo técnico asignado, incidente Asignado o En progreso. **Componente:** `CloseDialogComponent` (`features/incidents/close-dialog`).
- **Objetivo:** cerrar el incidente sin resolverlo, registrando el motivo.
- **Wireframe**
  ```
  Cerrar sin solución
  Selecciona un motivo para registrar el cierre del incidente.
  [ Motivo de cierre ▾ ]   (Duplicado / No reproducible / Descartado)
                                  [Cancelar] [Confirmar cierre]
  ```
- **Componentes que aparecen:** `R16 Dialog` (`lg`) · `R15 Select` · `R13 Button` outline y destructive.
- **Reutilizables usados:** R16, R15, R13.
- **Específicos:** `CloseDialogComponent`.
- **Datos y estado:** `reason: ClosureReason | null`. Valores: `Duplicado` (`DUPLICATE`), `No reproducible` (`NOT_REPRODUCIBLE`), `Descartado` (`DISCARDED`).
- **Interacciones:** **Confirmar cierre** (variante destructiva; **deshabilitado sin motivo**) → `incidents.close(id, reason)` → `Cerrado sin solución`, `closureReason`, `closedAt`. Cancelar cierra.
- **Reglas de negocio:** el cierre es definitivo; nadie puede reabrir. Desde Asignado o En progreso.
- **Textos exactos:** título "Cerrar sin solución"; descripción "Selecciona un motivo para registrar el cierre del incidente."; placeholder "Motivo de cierre"; botones "Cancelar" y "Confirmar cierre".
- **Nota:** en el mockup Confirmar solo cierra el diálogo (no cambia el estado, ni exige motivo).
- **Criterios de aceptación:** (1) Confirmar deshabilitado sin motivo; (2) tras confirmar, el incidente aparece como "Cerrado sin solución" con su motivo y sin acciones; (3) sigue visible en el listado del técnico.

---

### 13.6 Matriz resumen: vista × rol

| Vista | Supervisor | Técnico |
|---|:---:|:---:|
| V0 Selector de usuario | ✅ | ✅ |
| S1 Listado global | ✅ | ❌ |
| S2 Detalle (supervisor) | ✅ | ❌ |
| S3 Corregir clasificación | ✅ | ❌ |
| S4 Asignar / Reasignar | ✅ | ❌ |
| S5 Registrar incidente | ✅ | ❌ |
| S6 Administración · Equipo técnico | ✅ | ❌ |
| S7 Alta de técnico | ✅ | ❌ |
| T1 Mis asignados | ❌ | ✅ |
| T2 Detalle (técnico) | ❌ | ✅ (solo si es suyo) |
| T3 Panel de Análisis con IA | ❌ | ✅ |
| T4 Formulario de resolución | ❌ | ✅ |
| T5 Cerrar sin solución | ❌ | ✅ |

> `/incidentes/:id` es una sola ruta con **dos variantes** (S2 y T2) que decide el componente según el rol del usuario activo; los bloques comunes (`IncidentSummaryComponent`, cabecera) se comparten.
