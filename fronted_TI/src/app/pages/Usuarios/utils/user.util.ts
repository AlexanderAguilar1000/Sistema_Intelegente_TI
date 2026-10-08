import { AppUser, ApiRole, UserApiResponse, UserRole } from '../models/user.model';

const ROLE_LABEL: Record<ApiRole, UserRole> = {
  SUPERVISOR: 'Supervisor',
  TECHNICIAN: 'Técnico',
};

const AREA_LABEL: Record<string, string> = {
  INFRASTRUCTURE: 'Infraestructura',
  APPLICATIONS: 'Aplicaciones',
  DATABASE: 'Base de datos',
  SECURITY: 'Seguridad',
  USER_SUPPORT: 'Soporte a usuario',
};

export function toAppUser(dto: UserApiResponse): AppUser {
  return {
    id: dto.id,
    fullName: dto.fullName,
    role: ROLE_LABEL[dto.role],
    area: dto.area ? (AREA_LABEL[dto.area] ?? dto.area) : undefined,
  };
}

/** "Técnico · Infraestructura" o "Supervisor" */
export function roleLine(user: AppUser): string {
  return user.area ? `${user.role} · ${user.area}` : user.role;
}

export function homeRouteFor(user: AppUser): string {
  return user.role === 'Supervisor' ? '/incidentes' : '/mis-asignados';
}
