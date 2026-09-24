import { Injectable, computed, signal } from '@angular/core';
import { NewTechnician, User } from '../models/user.model';

const SEED_USERS: User[] = [
  { id: 'u1', fullName: 'Ana García', username: 'ana.garcia', role: 'Supervisor' },
  { id: 'u2', fullName: 'Carlos Ruiz', username: 'carlos.ruiz', role: 'Técnico', area: 'Infraestructura' },
  { id: 'u3', fullName: 'María López', username: 'maria.lopez', role: 'Técnico', area: 'Aplicaciones' },
  { id: 'u4', fullName: 'Pedro Sánchez', username: 'pedro.sanchez', role: 'Técnico', area: 'Infraestructura' },
];

@Injectable({ providedIn: 'root' })
export class UserService {
  readonly users = signal<User[]>(SEED_USERS);
  readonly technicians = computed(() => this.users().filter((u) => u.role === 'Técnico'));

  usernameExists(username: string): boolean {
    const value = username.trim().toLowerCase();
    return this.users().some((u) => u.username.toLowerCase() === value);
  }

  addTechnician(data: NewTechnician): void {
    const technician: User = {
      id: 'u' + Date.now(),
      fullName: data.fullName.trim(),
      username: data.username.trim().toLowerCase(),
      role: 'Técnico',
      area: data.area,
    };
    this.users.update((list) => [...list, technician]);
  }
}
