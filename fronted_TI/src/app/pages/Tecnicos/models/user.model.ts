export type UserRole = 'Supervisor' | 'Técnico';

export type TechArea =
  | 'Infraestructura'
  | 'Aplicaciones'
  | 'Base de datos'
  | 'Seguridad'
  | 'Soporte a usuario';

export interface User {
  id: string;
  fullName: string;
  username: string;
  role: UserRole;
  area?: TechArea;
}

export interface NewTechnician {
  fullName: string;
  username: string;
  area: TechArea;
}
