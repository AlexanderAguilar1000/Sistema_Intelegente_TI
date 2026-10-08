export type ApiRole = 'SUPERVISOR' | 'TECHNICIAN';
export type UserRole = 'Supervisor' | 'Técnico';

/** Forma que devuelve GET /api/users */
export interface UserApiResponse {
  id: number;
  fullName: string;
  role: ApiRole;
  area: string | null;
}

/** Usuario listo para pintar en la UI */
export interface AppUser {
  id: number;
  fullName: string;
  role: UserRole;
  area?: string;
}
