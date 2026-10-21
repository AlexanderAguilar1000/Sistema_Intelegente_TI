import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../../Usuarios/services/auth.service';
import { UsuariosService } from '../../../Usuarios/services/user.service';
import { AiRecommendationPanelComponent } from '../../components/ai-recommendation-panel/ai-recommendation-panel.component';
import { AssignDialogComponent } from '../../components/assign-dialog/assign-dialog.component';
import { ClassificationDialogComponent } from '../../components/classification-dialog/classification-dialog.component';
import { IncidentSummaryComponent } from '../../components/incident-summary/incident-summary.component';
import { IncidentDetail, IncidentStatus } from '../../models/incident.model';
import { StatusBadgeComponent } from '../../components/status-badge/status-badge.component';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-incident-detail-page',
  standalone: true,
  imports: [DatePipe, RouterLink, StatusBadgeComponent, IncidentSummaryComponent, ClassificationDialogComponent, AssignDialogComponent, AiRecommendationPanelComponent],
  templateUrl: './incident-detail-page.component.html',
  styleUrl: './incident-detail-page.component.css'
})
export class IncidentDetailPageComponent {
  private route = inject(ActivatedRoute);
  private service = inject(IncidentService);
  private usuarios = inject(UsuariosService);
  private auth = inject(AuthService);

  error = signal<string | null>(null);
  classificationOpen = signal(false);
  assignOpen = signal(false);
  /** Detalle devuelto por el PATCH de clasificación; tiene prioridad sobre el cargado hasta cambiar de ruta. */
  private updated = signal<IncidentDetail | null>(null);

  /** undefined = cargando · null = no se pudo obtener (ver `error`). */
  private loaded = toSignal(
    this.route.paramMap.pipe(
      map((params) => Number(params.get('id'))),
      tap(() => {
        this.error.set(null);
        this.updated.set(null);
      }),
      switchMap((id) =>
        this.service.getDetail(id).pipe(
          catchError((err: HttpErrorResponse) => {
            this.error.set(this.errorMessage(err));
            return of(null);
          }),
        ),
      ),
    ),
  );

  incident = computed(() => this.updated() ?? this.loaded());

  // --- Variante técnico (solo frontend: aún sin conexión al backend) ---
  /** Técnico asignado a este incidente. */
  isTech = computed(() => {
    const user = this.auth.activeUser();
    return user?.role === 'Técnico' && !!user && user.id === this.incident()?.assignedTo;
  });
  /** "Iniciar trabajo" se refleja solo en pantalla hasta tener endpoint. */
  private started = signal(false);
  aiOpen = signal(false);

  status = computed<IncidentStatus | undefined>(() => {
    const status = this.incident()?.status;
    return status === 'Asignado' && this.started() ? 'En progreso' : status;
  });
  canStart = computed(() => this.isTech() && this.status() === 'Asignado');
  canAnalyze = computed(() => this.isTech() && (this.status() === 'Asignado' || this.status() === 'En progreso'));
  canResolve = computed(() => this.isTech() && this.status() === 'En progreso');
  canClose = computed(() => this.canAnalyze());

  startWork(): void {
    this.started.set(true);
  }

  analyzeWithAi(): void {
    this.aiOpen.set(true);
  }

  constructor() {
    // El detalle trae ids de usuario; los nombres salen de GET /api/users.
    if (!this.usuarios.users().length) this.usuarios.load();
  }

  createdByName = computed(() => this.userName(this.incident()?.createdBy));
  assignedToName = computed(() => {
    const id = this.incident()?.assignedTo;
    return id == null ? undefined : this.userName(id);
  });

  assignedToArea = computed(() => {
    const id = this.incident()?.assignedTo;
    return id == null ? undefined : this.usuarios.users().find((u) => u.id === id)?.area;
  });

  /** Resuelto o Cerrado sin solución: solo lectura. */
  isFinal = computed(() => {
    const status = this.incident()?.status;
    return status === 'Resuelto' || status === 'Cerrado sin solución';
  });
  /** Acciones del supervisor: cualquier usuario que no sea técnico. */
  canEdit = computed(() => !!this.incident() && !this.isFinal() && this.auth.activeUser()?.role !== 'Técnico');
  canRetry = computed(() => this.canEdit() && !!this.incident()?.pendingClassification);
  isReassign = computed(() => !!this.incident()?.assignedTo);
  /** Asignar exige clasificación previa. */
  assignDisabled = computed(() => !this.isReassign() && (!!this.incident()?.pendingClassification || !this.incident()?.area));

  onAssigned(detail: IncidentDetail): void {
    this.updated.set(detail);
  }

  onClassificationSaved(detail: IncidentDetail): void {
    this.updated.set(detail);
  }

  private userName(id: number | undefined): string {
    if (id == null) return '—';
    return this.usuarios.users().find((u) => u.id === id)?.fullName ?? `Usuario #${id}`;
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 404) return 'No se encontró el incidente solicitado.';
    if (err.status === 403) return err.error?.message ?? 'Solo puedes ver los incidentes que tienes asignados.';
    return 'No se pudo cargar el incidente.';
  }
}
