import { Component, computed, input } from '@angular/core';
import { IncidentKpi } from '../../models/incident.model';

/** Trazos decorativos del mini-gráfico (viewBox 110x36) según el tipo de indicador. */
const SPARK_PATHS: Record<IncidentKpi['icon'], string> = {
  'clipboard-list': 'M0 32 H38 C46 32 48 4 56 4 C64 4 66 32 74 32 H110',
  'user-check': 'M0 32 H38 C46 32 48 4 56 4 C64 4 66 32 74 32 H110',
  activity: 'M0 32 C8 32 10 6 18 6 C26 6 28 32 36 32 C44 32 46 6 54 6 C62 6 64 32 72 32 C80 32 82 6 90 6 C98 6 100 32 110 32',
  'shield-check': 'M0 32 H8 C16 32 18 16 26 16 C34 16 36 32 44 32 C52 32 56 4 66 4 C76 4 80 32 88 32 H110',
  'alert-circle': 'M0 32 H62 C70 32 72 4 80 4 C88 4 90 32 98 32 H110',
};

@Component({
  selector: 'app-stat-card',
  standalone: true,
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.css'
})
export class StatCardComponent {
  kpi = input.required<IncidentKpi>();
  period = input('');

  sparkPath = computed(() => SPARK_PATHS[this.kpi().icon]);
}
