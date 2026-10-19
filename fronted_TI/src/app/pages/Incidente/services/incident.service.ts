import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import {
  Incident,
  IncidentApiResponse,
  IncidentDetail,
  IncidentDetailApiResponse,
  IncidentListApiResponse,
} from '../models/incident.model';
import { toIncident, toIncidentDetail } from '../utils/incident-api.util';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private http = inject(HttpClient);

  readonly incidents = signal<Incident[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** POST /api/incidents: registra el incidente; el backend lo clasifica con IA. */
  createIncident(body: { title: string; description: string }): Observable<Incident> {
    return this.http.post<IncidentApiResponse>('/api/incidents', body).pipe(
      map(toIncident),
      tap((incident) => this.incidents.update((list) => [incident, ...list])),
    );
  }

  /** GET /api/incidents: el backend ya devuelve los incidentes ordenados por creación descendente. */
  loadIncidents(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<IncidentListApiResponse>('/api/incidents').subscribe({
      next: (res) => {
        this.incidents.set(res.incidents.map(toIncident));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de incidentes.');
        this.loading.set(false);
      },
    });
  }

  /** GET /api/incidents/{id}: 404 si no existe, 403 si el técnico no es el asignado. */
  getDetail(id: number): Observable<IncidentDetail> {
    return this.http.get<IncidentDetailApiResponse>(`/api/incidents/${id}`).pipe(map(toIncidentDetail));
  }
}
