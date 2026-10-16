import { Component, input } from '@angular/core';
import { IncidentKpi } from '../../models/incident.model';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.css'
})
export class StatCardComponent {
  kpi = input.required<IncidentKpi>();
  period = input('');
}
