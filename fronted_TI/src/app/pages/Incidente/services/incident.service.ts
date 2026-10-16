import { Injectable, computed, signal } from '@angular/core';
import { Incident, IncidentUser } from '../models/incident.model';
import { sortByCreatedDesc } from '../utils/incident.util';
import { MOCK_INCIDENTS, MOCK_USERS } from './incident.mock';

/** Incidentes con datos falsos; se sustituirá por llamadas a /api/incidents. */
@Injectable({ providedIn: 'root' })
export class IncidentService {
  readonly incidents = signal<Incident[]>(sortByCreatedDesc(MOCK_INCIDENTS));
  readonly users = signal<IncidentUser[]>(MOCK_USERS);

  private readonly fullNameByUsername = computed(
    () => new Map(this.users().map((u) => [u.username, u.fullName])),
  );

  /** Nombre completo del técnico o undefined si no está asignado / no existe. */
  technicianName(username?: string): string | undefined {
    return username ? this.fullNameByUsername().get(username) : undefined;
  }
}
