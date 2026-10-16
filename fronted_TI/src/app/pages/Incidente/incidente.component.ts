import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IncidentTableComponent } from './components/incident-table/incident-table.component';
import { StatCardComponent } from './components/stat-card/stat-card.component';
import { Incident } from './models/incident.model';
import { IncidentService } from './services/incident.service';
import { KPI_PERIOD, buildKpis } from './utils/incident.util';

@Component({
  selector: 'app-incidente',
  standalone: true,
  imports: [RouterLink, StatCardComponent, IncidentTableComponent],
  templateUrl: './incidente.component.html',
  styleUrl: './incidente.component.css'
})
export class IncidenteComponent {
  private service = inject(IncidentService);
  private router = inject(Router);

  incidents = this.service.incidents;
  users = this.service.users;
  kpis = computed(() => buildKpis(this.incidents()));
  period = KPI_PERIOD.toUpperCase();

  openIncident(incident: Incident): void {
    this.router.navigate(['/incidentes', incident.id]);
  }
}
