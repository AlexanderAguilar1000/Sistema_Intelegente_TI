import { Incident, IncidentApiResponse, IncidentStatus, IncidentType, Priority, TechArea } from '../models/incident.model';

const STATUS_LABEL: Record<string, IncidentStatus> = {
  REGISTERED: 'Registrado',
  ASSIGNED: 'Asignado',
  IN_PROGRESS: 'En progreso',
  RESOLVED: 'Resuelto',
  CLOSED_WITHOUT_SOLUTION: 'Cerrado sin solución',
};

const PRIORITY_LABEL: Record<string, Priority> = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja',
};

const TYPE_LABEL: Record<string, IncidentType> = {
  BUG: 'Bug',
  PERFORMANCE: 'Rendimiento',
  ACCESS: 'Acceso',
  HARDWARE: 'Hardware',
  NETWORK: 'Red',
  CONFIGURATION: 'Configuración',
};

const AREA_LABEL: Record<string, TechArea> = {
  INFRASTRUCTURE: 'Infraestructura',
  APPLICATIONS: 'Aplicaciones',
  DATABASE: 'Base de datos',
  SECURITY: 'Seguridad',
  USER_SUPPORT: 'Soporte a usuario',
};

/** 412 + 2024 -> "INC-2024-0412". */
export function toIncidentCode(id: number, createdAt: string): string {
  const year = new Date(createdAt).getFullYear();
  return `INC-${year}-${String(id).padStart(4, '0')}`;
}

export function toIncident(dto: IncidentApiResponse): Incident {
  return {
    id: dto.id,
    code: toIncidentCode(dto.id, dto.createdAt),
    title: dto.title,
    description: dto.description,
    status: STATUS_LABEL[dto.status] ?? 'Registrado',
    type: dto.type ? TYPE_LABEL[dto.type] : undefined,
    priority: dto.priority ? PRIORITY_LABEL[dto.priority] : undefined,
    area: dto.area ? AREA_LABEL[dto.area] : undefined,
    createdAt: dto.createdAt,
    assignedToName: dto.assignedToName ?? undefined,
    pendingClassification: dto.classificationStatus === 'PENDING',
  };
}
