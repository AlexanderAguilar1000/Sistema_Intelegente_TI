import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { CreateTechnicianRequest, NewTechnician, TechnicianApiResponse, User } from '../models/user.model';
import { toAreaCode, toTechnician } from '../utils/user.util';

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

  createTechnician(data: NewTechnician): Observable<User> {
    const body: CreateTechnicianRequest = {
      fullName: data.fullName.trim(),
      username: data.username.trim().toLowerCase(),
      area: toAreaCode(data.area),
    };
    return this.http.post<TechnicianApiResponse>('/api/technicians', body).pipe(
      map((dto) => ({ ...toTechnician(dto), username: body.username })),
      tap((technician) => this.technicians.update((list) => [...list, technician])),
    );
  }
}
