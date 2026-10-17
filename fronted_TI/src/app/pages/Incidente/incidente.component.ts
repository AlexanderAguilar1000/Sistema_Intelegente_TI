import { Component, computed, effect, inject, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../Usuarios/services/auth.service';
import { IncidentTableComponent } from './components/incident-table/incident-table.component';
import { StatCardComponent } from './components/stat-card/stat-card.component';
import { Incident } from './models/incident.model';
import { IncidentService } from './services/incident.service';
import { buildKpis, currentPeriod } from './utils/incident.util';

@Component({
  selector: 'app-incidente',
  standalone: true,
  imports: [RouterLink, StatCardComponent, IncidentTableComponent],
  templateUrl: './incidente.component.html',
  styleUrl: './incidente.component.css'
})
export class IncidenteComponent {
  private service = inject(IncidentService);
  private auth = inject(AuthService);
  private router = inject(Router);

  incidents = this.service.incidents;
  loading = this.service.loading;
  error = this.service.error;
  kpis = computed(() => buildKpis(this.incidents()));
  period = currentPeriod().toUpperCase();

  constructor() {
    // Recarga cuando hay (o cambia) el usuario activo: la API exige X-User-Id.
    effect(() => {
      if (this.auth.activeUser()) untracked(() => this.service.loadIncidents());
    });
  }

  openIncident(incident: Incident): void {
    this.router.navigate(['/incidentes', incident.id]);
  }
}
