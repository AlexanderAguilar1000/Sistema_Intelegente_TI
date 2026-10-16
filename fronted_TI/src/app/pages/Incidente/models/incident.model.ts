export type Priority = 'Alta' | 'Media' | 'Baja';
export type IncidentType = 'Bug' | 'Rendimiento' | 'Acceso' | 'Hardware' | 'Red' | 'Configuración';
export type TechArea = 'Infraestructura' | 'Aplicaciones' | 'Base de datos' | 'Seguridad' | 'Soporte a usuario';
export type IncidentStatus = 'Registrado' | 'Asignado' | 'En progreso' | 'Resuelto' | 'Cerrado sin solución';

export interface Incident {
  /** Formato INC-AAAA-NNNN */
  id: string;
  title: string;
  description: string;
  status: IncidentStatus;
  type?: IncidentType;
  priority?: Priority;
  area?: TechArea;
  /** username del supervisor que lo registró */
  createdBy: string;
  /** Fecha ISO */
  createdAt: string;
  /** username del técnico asignado */
  assignedTo?: string;
  resolvedAt?: string;
  /** Sin tipo/prioridad/área todavía */
  pendingClassification?: boolean;
}

/** Técnico o supervisor, para resolver el nombre completo por username. */
export interface IncidentUser {
  id: string;
  fullName: string;
  username: string;
  role: 'Supervisor' | 'Técnico';
  area?: TechArea;
}

export interface IncidentKpi {
  label: string;
  value: string;
  helper: string;
  icon: 'clipboard-list' | 'activity' | 'shield-check' | 'alert-circle';
  tone: 'blue' | 'amber' | 'green' | 'red';
}

export type StatusFilter = 'all' | IncidentStatus;
export type PriorityFilter = 'all' | Priority;
