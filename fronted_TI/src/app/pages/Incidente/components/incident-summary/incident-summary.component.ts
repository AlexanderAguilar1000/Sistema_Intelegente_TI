import { Component, input } from '@angular/core';
import { IncidentDetail } from '../../models/incident.model';
import { PriorityBadgeComponent } from '../priority-badge/priority-badge.component';

@Component({
  selector: 'app-incident-summary',
  standalone: true,
  imports: [PriorityBadgeComponent],
  templateUrl: './incident-summary.component.html',
  styleUrl: './incident-summary.component.css'
})
export class IncidentSummaryComponent {
  incident = input.required<IncidentDetail>();
}
