export type UserRole = 'Supervisor' | 'Técnico';

export type TechArea =
  | 'Infraestructura'
  | 'Aplicaciones'
  | 'Base de datos'
  | 'Seguridad'
  | 'Soporte a usuario';

/** Forma que devuelve GET /api/technicians/all */
export interface TechnicianApiResponse {
  id: number;
  fullName: string;
  role: string;
  area: string | null;
}

export interface User {
  id: number;
  fullName: string;
  /** El endpoint no lo devuelve; solo existe en técnicos dados de alta localmente. */
  username?: string;
  role: UserRole;
  area?: string;
}

export interface NewTechnician {
  fullName: string;
  username: string;
  area: TechArea;
}
