import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { UsuariosService } from '../../../Usuarios/services/user.service';
import { IncidentSummaryComponent } from '../../components/incident-summary/incident-summary.component';
import { StatusBadgeComponent } from '../../components/status-badge/status-badge.component';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-incident-detail-page',
  standalone: true,
  imports: [DatePipe, RouterLink, StatusBadgeComponent, IncidentSummaryComponent],
  templateUrl: './incident-detail-page.component.html',
  styleUrl: './incident-detail-page.component.css'
})
export class IncidentDetailPageComponent {
  private route = inject(ActivatedRoute);
  private service = inject(IncidentService);
  private usuarios = inject(UsuariosService);

  error = signal<string | null>(null);

  /** undefined = cargando · null = no se pudo obtener (ver `error`). */
  incident = toSignal(
    this.route.paramMap.pipe(
      map((params) => Number(params.get('id'))),
      tap(() => this.error.set(null)),
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

  constructor() {
    // El detalle trae ids de usuario; los nombres salen de GET /api/users.
    if (!this.usuarios.users().length) this.usuarios.load();
  }

  createdByName = computed(() => this.userName(this.incident()?.createdBy));
  assignedToName = computed(() => {
    const id = this.incident()?.assignedTo;
    return id == null ? undefined : this.userName(id);
  });

  /** Resuelto o Cerrado sin solución: solo lectura. */
  isFinal = computed(() => {
    const status = this.incident()?.status;
    return status === 'Resuelto' || status === 'Cerrado sin solución';
  });
  canEdit = computed(() => !!this.incident() && !this.isFinal());
  canRetry = computed(() => this.canEdit() && !!this.incident()?.pendingClassification);
  isReassign = computed(() => !!this.incident()?.assignedTo);
  /** Asignar exige clasificación previa. */
  assignDisabled = computed(() => !this.isReassign() && (!!this.incident()?.pendingClassification || !this.incident()?.area));

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
