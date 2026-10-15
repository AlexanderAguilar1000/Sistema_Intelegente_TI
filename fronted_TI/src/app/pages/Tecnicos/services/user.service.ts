import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { NewTechnician, TechnicianApiResponse, User } from '../models/user.model';
import { toTechnician } from '../utils/user.util';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  readonly technicians = signal<User[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  loadTechnicians(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<TechnicianApiResponse[]>('/api/technicians/all').subscribe({
      next: (list) => {
        this.technicians.set(list.map(toTechnician));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de técnicos.');
        this.loading.set(false);
      },
    });
  }

  usernameExists(username: string): boolean {
    const value = username.trim().toLowerCase();
    return this.technicians().some((u) => u.username?.toLowerCase() === value);
  }

  // El alta todavía no está conectada al backend (POST /api/technicians): solo agrega en memoria.
  addTechnician(data: NewTechnician): void {
    const technician: User = {
      id: Date.now(),
      fullName: data.fullName.trim(),
      username: data.username.trim().toLowerCase(),
      role: 'Técnico',
      area: data.area,
    };
    this.technicians.update((list) => [...list, technician]);
  }
}
