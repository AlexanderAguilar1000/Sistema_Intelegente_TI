import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../Usuarios/services/auth.service';
import { IncidentCreateDialogComponent } from './components/incident-create-dialog/incident-create-dialog.component';
import { IncidentTableComponent } from './components/incident-table/incident-table.component';
import { StatCardComponent } from './components/stat-card/stat-card.component';
import { Incident } from './models/incident.model';
import { IncidentService } from './services/incident.service';
import { mockIncidentsFor } from './utils/incident.mock';
import { buildKpis, buildTechnicianKpis, currentPeriod } from './utils/incident.util';

@Component({
  selector: 'app-incidente',
  standalone: true,
  imports: [StatCardComponent, IncidentTableComponent, IncidentCreateDialogComponent],
  templateUrl: './incidente.component.html',
  styleUrl: './incidente.component.css'
})
export class IncidenteComponent {
  private service = inject(IncidentService);
  private auth = inject(AuthService);
  private router = inject(Router);

  /** La ruta /mis-asignados reutiliza este componente con `data: { view: 'technician' }`. */
  readonly isTechnicianView = inject(ActivatedRoute).snapshot.data['view'] === 'technician';

  // Vista técnico: datos de prueba, aún sin backend.
  incidents = computed<Incident[]>(() => {
    if (!this.isTechnicianView) return this.service.incidents();
    const user = this.auth.activeUser();
    return user ? mockIncidentsFor(user.fullName) : [];
  });
  loading = computed(() => !this.isTechnicianView && this.service.loading());
  error = computed(() => (this.isTechnicianView ? null : this.service.error()));
  kpis = computed(() => (this.isTechnicianView ? buildTechnicianKpis(this.incidents()) : buildKpis(this.incidents())));
  userName = computed(() => this.auth.activeUser()?.fullName ?? '');
  period = currentPeriod().toUpperCase();
  createOpen = signal(false);

  constructor() {
    effect(() => {
      const user = this.auth.activeUser();
      if (!user) return;
      // El selector de usuario no navega: si el activo deja de ser técnico, sale de esta vista.
      if (this.isTechnicianView) {
        if (user.role !== 'Técnico') untracked(() => this.router.navigate(['/incidentes']));
        return;
      }
      // Recarga cuando hay (o cambia) el usuario activo: la API exige X-User-Id.
      untracked(() => this.service.loadIncidents());
    });
  }

  openIncident(incident: Incident): void {
    this.router.navigate(['/incidentes', incident.id]);
  }
}
