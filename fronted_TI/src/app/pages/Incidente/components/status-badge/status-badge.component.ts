import { Component, computed, input } from '@angular/core';
import { IncidentStatus } from '../../models/incident.model';

const STATUS_CLASS: Record<IncidentStatus, string> = {
  Registrado: 'registered',
  Asignado: 'assigned',
  'En progreso': 'in-progress',
  Resuelto: 'resolved',
  'Cerrado sin solución': 'closed',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  templateUrl: './status-badge.component.html',
  styleUrl: './status-badge.component.css'
})
export class StatusBadgeComponent {
  status = input.required<IncidentStatus>();
  cssClass = computed(() => STATUS_CLASS[this.status()]);
}
