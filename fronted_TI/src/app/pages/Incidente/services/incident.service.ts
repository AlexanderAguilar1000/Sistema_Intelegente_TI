import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subscription, map, tap } from 'rxjs';
import {
  ClassificationValues,
  ClassifyIncidentBody,
  ClosureReason,
  Incident,
  IncidentAnalysis,
  IncidentApiResponse,
  IncidentDetail,
  IncidentDetailApiResponse,
  IncidentListApiResponse,
  PriorityFilter,
} from '../models/incident.model';
import { AREA_CODE, PRIORITY_CODE, TYPE_CODE, toIncident, toIncidentDetail } from '../utils/incident-api.util';

const PAGE_SIZE = 20;
/** Tamaño máximo que admite el backend. */
const MAX_PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private http = inject(HttpClient);

  /** Incidentes de la página visible. */
  readonly incidents = signal<Incident[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  /** Página visible (empieza en 0) y totales de todas las páginas, tal como los da el backend. */
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly priorityFilter = signal<PriorityFilter>('all');
  /** Base de los indicadores, independiente de la página y del filtro de la tabla. */
  readonly kpiIncidents = signal<Incident[]>([]);

  private listRequest?: Subscription;
  private kpiRequest?: Subscription;

  /** POST /api/incidents: registra el incidente; el backend lo clasifica con IA. */
  createIncident(body: { title: string; description: string }): Observable<Incident> {
    return this.http.post<IncidentApiResponse>('/api/incidents', body).pipe(
      map(toIncident),
      // El incidente nuevo es el más reciente: se vuelve a pedir la primera página.
      tap(() => {
        this.loadIncidents(0, this.priorityFilter());
        this.loadKpiIncidents();
      }),
    );
  }

  /**
   * GET /api/incidents?page=&size=&priority=: el backend filtra, ordena por creación descendente y
   * pagina. Una petición nueva cancela la anterior para que no llegue una página atrasada.
   */
  loadIncidents(page = 0, priority: PriorityFilter = 'all'): void {
    this.listRequest?.unsubscribe();
    this.loading.set(true);
    this.error.set(null);
    this.page.set(page);
    this.priorityFilter.set(priority);

    let params = new HttpParams().set('page', page).set('size', PAGE_SIZE);
    if (priority !== 'all') params = params.set('priority', PRIORITY_CODE[priority]);

    this.listRequest = this.http.get<IncidentListApiResponse>('/api/incidents', { params }).subscribe({
      next: (res) => {
        this.incidents.set(res.incidents.map(toIncident));
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de incidentes.');
        this.loading.set(false);
      },
    });
  }

  /**
   * Provisional hasta T-53: los indicadores se calculan en el navegador con los incidentes más
   * recientes, como mucho {@link MAX_PAGE_SIZE}, porque el backend aún no los publica (T-48).
   */
  loadKpiIncidents(): void {
    this.kpiRequest?.unsubscribe();
    const params = new HttpParams().set('page', 0).set('size', MAX_PAGE_SIZE);
    this.kpiRequest = this.http.get<IncidentListApiResponse>('/api/incidents', { params }).subscribe({
      next: (res) => this.kpiIncidents.set(res.incidents.map(toIncident)),
      error: () => this.kpiIncidents.set([]),
    });
  }

  /** GET /api/incidents/{id}: 404 si no existe, 403 si el técnico no es el asignado. */
  getDetail(id: number): Observable<IncidentDetail> {
    return this.http.get<IncidentDetailApiResponse>(`/api/incidents/${id}`).pipe(map(toIncidentDetail));
  }

  /** PUT /api/incidents/{id}/assignment: solo supervisor. Devuelve el detalle actualizado (estado Asignado). */
  assignIncident(id: number, technicianId: number, technicianName: string): Observable<IncidentDetail> {
    return this.http.put<IncidentDetailApiResponse>(`/api/incidents/${id}/assignment`, { technicianId }).pipe(
      map(toIncidentDetail),
      tap((detail) =>
        this.incidents.update((list) =>
          list.map((i) => (i.id === id ? { ...i, status: detail.status, assignedToName: technicianName } : i)),
        ),
      ),
    );
  }

  /** POST /api/incidents/{id}/start: solo el técnico asignado. Devuelve el detalle actualizado (estado En progreso). */
  startIncident(id: number): Observable<IncidentDetail> {
    return this.http.post<IncidentDetailApiResponse>(`/api/incidents/${id}/start`, {}).pipe(
      map(toIncidentDetail),
      tap((detail) =>
        this.incidents.update((list) => list.map((i) => (i.id === id ? { ...i, status: detail.status } : i))),
      ),
    );
  }

  /** POST /api/incidents/{id}/resolution: solo el técnico asignado. Devuelve el detalle actualizado (estado Resuelto). */
  resolveIncident(id: number, solution: string): Observable<IncidentDetail> {
    return this.http.post<IncidentDetailApiResponse>(`/api/incidents/${id}/resolution`, { solution }).pipe(
      map(toIncidentDetail),
      tap((detail) =>
        this.incidents.update((list) => list.map((i) => (i.id === id ? { ...i, status: detail.status } : i))),
      ),
    );
  }

  /** POST /api/incidents/{id}/closure: solo el técnico asignado. Devuelve el detalle actualizado (Cerrado sin solución). */
  closeWithoutSolution(id: number, reason: ClosureReason): Observable<IncidentDetail> {
    return this.http.post<IncidentDetailApiResponse>(`/api/incidents/${id}/closure`, { reason }).pipe(
      map(toIncidentDetail),
      tap((detail) =>
        this.incidents.update((list) => list.map((i) => (i.id === id ? { ...i, status: detail.status } : i))),
      ),
    );
  }

  /** POST /api/incidents/{id}/analysis: pasos de diagnóstico y casos similares generados por IA. */
  analyzeIncident(id: number): Observable<IncidentAnalysis> {
    return this.http.post<IncidentAnalysis>(`/api/incidents/${id}/analysis`, {});
  }

  /** POST /api/incidents/{id}/classification/retry: reintenta la clasificación con IA. Devuelve el detalle actualizado. */
  retryClassification(id: number): Observable<IncidentDetail> {
    return this.http.post<IncidentDetailApiResponse>(`/api/incidents/${id}/classification/retry`, {}).pipe(
      map(toIncidentDetail),
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
                }
              : i,
          ),
        ),
      ),
    );
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
