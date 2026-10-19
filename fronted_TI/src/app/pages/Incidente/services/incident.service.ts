import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, delay, map, of, tap } from 'rxjs';
import { Incident, IncidentApiResponse, IncidentDetail, IncidentListApiResponse } from '../models/incident.model';
import { toIncident } from '../utils/incident-api.util';
import { findMockIncident } from '../utils/incident-detail.mock';

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

  /**
   * Detalle de un incidente. TODO: reemplazar por GET /api/incidents/{id}.
   * Por ahora usa datos falsos; si el id no está, cae al incidente del listado cargado.
   */
  getDetail(id: number): Observable<IncidentDetail | null> {
    const mock = findMockIncident(id);
    const fromList = this.incidents().find((i) => i.id === id);
    const detail: IncidentDetail | null =
      mock ?? (fromList ? { ...fromList, createdBy: 'Ana García', unindexed: false } : null);
    return of(detail).pipe(delay(250));
  }
}
