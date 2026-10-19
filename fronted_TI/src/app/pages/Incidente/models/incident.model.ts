export type Priority = 'Alta' | 'Media' | 'Baja';
export type IncidentType = 'Bug' | 'Rendimiento' | 'Acceso' | 'Hardware' | 'Red' | 'Configuración';
export type TechArea = 'Infraestructura' | 'Aplicaciones' | 'Base de datos' | 'Seguridad' | 'Soporte a usuario';
export type IncidentStatus = 'Registrado' | 'Asignado' | 'En progreso' | 'Resuelto' | 'Cerrado sin solución';

/** Forma de cada incidente en GET /api/incidents (enums como códigos del backend). */
export interface IncidentApiResponse {
  id: number;
  title: string;
  description: string;
  status: string;
  classificationStatus: string;
  type: string | null;
  priority: string | null;
  area: string | null;
  createdBy: number;
  assignedTo: number | null;
  assignedToName: string | null;
  createdAt: string;
}

/** Cuerpo de GET /api/incidents. */
export interface IncidentListApiResponse {
  incidents: IncidentApiResponse[];
  empty: boolean;
  message: string | null;
}

/** Incidente listo para la vista (etiquetas en español). */
export interface Incident {
  id: number;
  /** Código visible, formato INC-AAAA-NNNN. */
  code: string;
  title: string;
  description: string;
  status: IncidentStatus;
  type?: IncidentType;
  priority?: Priority;
  area?: TechArea;
  /** Fecha ISO */
  createdAt: string;
  assignedToName?: string;
  /** Sin tipo/prioridad/área todavía */
  pendingClassification: boolean;
}

/** Cuerpo de GET /api/incidents/{id} (enums como códigos del backend). */
export interface IncidentDetailApiResponse {
  id: number;
  title: string;
  description: string;
  status: string;
  classificationStatus: string;
  type: string | null;
  priority: string | null;
  area: string | null;
  createdBy: number;
  assignedTo: number | null;
  assignedBy: number | null;
  solution: string | null;
  closureReason: string | null;
  createdAt: string;
  assignedAt: string | null;
  startedAt: string | null;
  closedAt: string | null;
}

/** Detalle de un incidente (vista S2). El backend envía ids de usuario, no nombres. */
export interface IncidentDetail extends Incident {
  createdBy: number;
  assignedTo?: number;
  assignedBy?: number;
  /** "Clasificado por IA" | "Corregida manualmente"; sin valor si está pendiente. */
  classificationSource?: string;
  solution?: string;
  closureReason?: string;
  assignedAt?: string;
  startedAt?: string;
  closedAt?: string;
}

/** Cuerpo de PATCH /api/incidents/{id}/classification (códigos de catálogo del backend). */
export interface ClassifyIncidentBody {
  type: string;
  priority: string;
  area: string;
}

/** Valores elegidos en el diálogo "Corregir clasificación" (etiquetas en español). */
export interface ClassificationValues {
  type: IncidentType;
  priority: Priority;
  area: TechArea;
}

export const INCIDENT_TYPES: IncidentType[] = ['Bug', 'Rendimiento', 'Acceso', 'Hardware', 'Red', 'Configuración'];
export const PRIORITIES: Priority[] = ['Alta', 'Media', 'Baja'];
export const TECH_AREAS: TechArea[] = ['Infraestructura', 'Aplicaciones', 'Base de datos', 'Seguridad', 'Soporte a usuario'];

export interface IncidentKpi {
  label: string;
  value: string;
  helper: string;
  icon: 'clipboard-list' | 'activity' | 'shield-check' | 'alert-circle';
  tone: 'blue' | 'amber' | 'green' | 'red';
}

export type StatusFilter = 'all' | IncidentStatus;
export type PriorityFilter = 'all' | Priority;
