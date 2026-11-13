import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { CLOSURE_REASONS, IncidentDetail } from '../../models/incident.model';

/** Resultado de un incidente terminado: solución aplicada (Resuelto) o motivo del cierre (Cerrado sin solución). */
@Component({
  selector: 'app-incident-outcome',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './incident-outcome.component.html',
  styleUrl: './incident-outcome.component.css'
})
export class IncidentOutcomeComponent {
  incident = input.required<IncidentDetail>();
  /** Nombre del técnico que cerró el incidente (el asignado). */
  resolverName = input<string | undefined>();

  variant = computed<'resolved' | 'closed' | null>(() => {
    const status = this.incident().status;
    if (status === 'Resuelto') return 'resolved';
    if (status === 'Cerrado sin solución') return 'closed';
    return null;
  });

  /** Etiqueta y descripción del motivo; si llega un código desconocido se muestra tal cual. */
  reason = computed(() => {
    const code = this.incident().closureReason;
    if (!code) return null;
    return CLOSURE_REASONS.find((r) => r.value === code) ?? { value: code, label: code, description: '' };
  });
}
