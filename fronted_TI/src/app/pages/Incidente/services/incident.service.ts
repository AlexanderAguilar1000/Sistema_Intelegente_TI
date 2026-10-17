import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Incident, IncidentListApiResponse } from '../models/incident.model';
import { toIncident } from '../utils/incident-api.util';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private http = inject(HttpClient);

  readonly incidents = signal<Incident[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

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
}
