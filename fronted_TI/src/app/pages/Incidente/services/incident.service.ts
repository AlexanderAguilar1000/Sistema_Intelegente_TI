import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import {
  ClassificationValues,
  ClassifyIncidentBody,
  Incident,
  IncidentApiResponse,
  IncidentDetail,
  IncidentDetailApiResponse,
  IncidentListApiResponse,
} from '../models/incident.model';
import { AREA_CODE, PRIORITY_CODE, TYPE_CODE, toIncident, toIncidentDetail } from '../utils/incident-api.util';

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

  /** PATCH /api/incidents/{id}/classification: solo supervisor. Devuelve el detalle ya actualizado. */
  correctClassification(id: number, values: ClassificationValues): Observable<IncidentDetail> {
    const body: ClassifyIncidentBody = {
      type: TYPE_CODE[values.type],
      priority: PRIORITY_CODE[values.priority],
      area: AREA_CODE[values.area],
    };
    return this.http.patch<IncidentDetailApiResponse>(`/api/incidents/${id}/classification`, body).pipe(
      map(toIncidentDetail),
      // Mantiene el listado en sincronía (clasificación y, si el área excluyó al técnico, estado/asignación).
      tap((detail) =>
        this.incidents.update((list) =>
          list.map((i) =>
            i.id === id
              ? {
                  ...i,
                  status: detail.status,
                  type: detail.type,
                  priority: detail.priority,
                  area: detail.area,
                  pendingClassification: detail.pendingClassification,
                  assignedToName: detail.assignedTo ? i.assignedToName : undefined,
                }
              : i,
          ),
        ),
      ),
    );
  }
}
