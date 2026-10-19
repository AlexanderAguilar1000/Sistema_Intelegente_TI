import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map, switchMap } from 'rxjs';
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

  /** undefined = cargando · null = no encontrado. */
  incident = toSignal(
    this.route.paramMap.pipe(
      map((params) => Number(params.get('id'))),
      switchMap((id) => this.service.getDetail(id)),
    ),
  );

  /** Resuelto o Cerrado sin solución: solo lectura. */
  isFinal = computed(() => {
    const status = this.incident()?.status;
    return status === 'Resuelto' || status === 'Cerrado sin solución';
  });
  canEdit = computed(() => !!this.incident() && !this.isFinal());
  canRetry = computed(() => this.canEdit() && !!this.incident()?.pendingClassification);
  isReassign = computed(() => !!this.incident()?.assignedToName);
  /** Asignar exige clasificación previa. */
  assignDisabled = computed(() => !this.isReassign() && (!!this.incident()?.pendingClassification || !this.incident()?.area));
  canReindex = computed(() => this.incident()?.status === 'Resuelto' && !!this.incident()?.unindexed);
}
