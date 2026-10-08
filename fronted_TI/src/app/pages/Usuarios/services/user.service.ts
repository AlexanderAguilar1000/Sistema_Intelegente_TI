import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { AppUser, UserApiResponse } from '../models/user.model';
import { toAppUser } from '../utils/user.util';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private http = inject(HttpClient);

  readonly users = signal<AppUser[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<UserApiResponse[]>('/api/users').subscribe({
      next: (list) => {
        this.users.set(list.map(toAppUser));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de usuarios.');
        this.loading.set(false);
      },
    });
  }
}
