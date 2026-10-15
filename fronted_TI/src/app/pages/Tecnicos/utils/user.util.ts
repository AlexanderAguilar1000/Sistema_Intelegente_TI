import { TechnicianApiResponse, User } from '../models/user.model';

const AREA_LABEL: Record<string, string> = {
  INFRASTRUCTURE: 'Infraestructura',
  APPLICATIONS: 'Aplicaciones',
  DATABASE: 'Base de datos',
  SECURITY: 'Seguridad',
  USER_SUPPORT: 'Soporte a usuario',
};

export function toTechnician(dto: TechnicianApiResponse): User {
  return {
    id: dto.id,
    fullName: dto.fullName,
    role: 'Técnico',
    area: dto.area ? (AREA_LABEL[dto.area] ?? dto.area) : undefined,
  };
}

export const USERNAME_PATTERN = /^[a-z]+(\.[a-z]+)+$/;

export function getInitials(fullName: string): string {
  return fullName
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

export function toCorporateEmail(username: string): string {
  return `${username}@empresa.es`;
}
