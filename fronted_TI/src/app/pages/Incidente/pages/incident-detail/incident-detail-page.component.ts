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
import { CloseDialogComponent } from '../../components/close-dialog/close-dialog.component';
import { ResolveDialogComponent } from '../../components/resolve-dialog/resolve-dialog.component';
import { IncidentOutcomeComponent } from '../../components/incident-outcome/incident-outcome.component';
import { IncidentSummaryComponent } from '../../components/incident-summary/incident-summary.component';
import { IncidentAnalysis, IncidentDetail, IncidentStatus } from '../../models/incident.model';
import { StatusBadgeComponent } from '../../components/status-badge/status-badge.component';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-incident-detail-page',
  standalone: true,
  imports: [DatePipe, RouterLink, StatusBadgeComponent, IncidentSummaryComponent, IncidentOutcomeComponent, ClassificationDialogComponent, AssignDialogComponent, ResolveDialogComponent, CloseDialogComponent, AiRecommendationPanelComponent],
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
  resolveOpen = signal(false);
  closeOpen = signal(false);
  // Deben declararse antes de `loaded`: su tap las usa en cuanto se suscribe.
  aiOpen = signal(false);
  analyzing = signal(false);
  analysis = signal<IncidentAnalysis | null>(null);
  analysisError = signal<string | null>(null);
  /** Detalle devuelto por el PATCH de clasificación; tiene prioridad sobre el cargado hasta cambiar de ruta. */
  private updated = signal<IncidentDetail | null>(null);

  /** undefined = cargando · null = no se pudo obtener (ver `error`). */
  private loaded = toSignal(
    this.route.paramMap.pipe(
      map((params) => Number(params.get('id'))),
      tap(() => {
        this.error.set(null);
        this.updated.set(null);
        this.aiOpen.set(false);
        this.analysis.set(null);
        this.analysisError.set(null);
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
  starting = signal(false);
  retrying = signal(false);
  actionError = signal<string | null>(null);

  status = computed<IncidentStatus | undefined>(() => this.incident()?.status);
  canStart = computed(() => this.isTech() && this.status() === 'Asignado');
  canAnalyze = computed(() => this.isTech() && (this.status() === 'Asignado' || this.status() === 'En progreso'));
  canResolve = computed(() => this.isTech() && this.status() === 'En progreso');
  canClose = computed(() => this.canAnalyze());


  startWork(): void {
    const inc = this.incident();
    if (!inc || this.starting()) return;
    this.starting.set(true);
    this.actionError.set(null);
    this.service.startIncident(inc.id).subscribe({
      next: (detail) => {
        this.updated.set(detail);
        this.starting.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(err.error?.message ?? 'No se pudo iniciar el trabajo del incidente.');
        this.starting.set(false);
      },
    });
  }

  retryClassification(): void {
    const inc = this.incident();
    if (!inc || this.retrying()) return;
    this.retrying.set(true);
    this.actionError.set(null);
    this.service.retryClassification(inc.id).subscribe({
      next: (detail) => {
        this.updated.set(detail);
        this.retrying.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.actionError.set(err.error?.message ?? 'No se pudo reintentar la clasificación con IA.');
        this.retrying.set(false);
      },
    });
  }

  analyzeWithAi(): void {
    const inc = this.incident();
    if (!inc || this.analyzing()) return;
    this.aiOpen.set(true);
    this.analyzing.set(true);
    this.analysisError.set(null);
    this.service.analyzeIncident(inc.id).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.analyzing.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.analysis.set(null);
        this.analysisError.set(err.error?.message ?? 'No se pudo obtener el análisis de la IA.');
        this.analyzing.set(false);
      },
    });
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

  onResolved(detail: IncidentDetail): void {
    this.updated.set(detail);
  }

  onClosed(detail: IncidentDetail): void {
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
