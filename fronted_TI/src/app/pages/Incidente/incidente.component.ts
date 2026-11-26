import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../Usuarios/services/auth.service';
import { IncidentCreateDialogComponent } from './components/incident-create-dialog/incident-create-dialog.component';
import { IncidentTableComponent } from './components/incident-table/incident-table.component';
import { StatCardComponent } from './components/stat-card/stat-card.component';
import { Incident, PriorityFilter } from './models/incident.model';
import { IncidentService } from './services/incident.service';
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

  // GET /api/incidents ya devuelve solo los asignados cuando el usuario activo es técnico.
  incidents = this.service.incidents;
  loading = this.service.loading;
  error = this.service.error;
  page = this.service.page;
  totalPages = this.service.totalPages;
  totalElements = this.service.totalElements;
  priority = this.service.priorityFilter;
  // No usan la página visible: así no cambian al paginar ni al filtrar.
  kpis = computed(() => {
    const incidents = this.service.kpiIncidents();
    return this.isTechnicianView ? buildTechnicianKpis(incidents) : buildKpis(incidents);
  });
  userName = computed(() => this.auth.activeUser()?.fullName ?? '');
  period = currentPeriod().toUpperCase();
  createOpen = signal(false);

  constructor() {
    effect(() => {
      const user = this.auth.activeUser();
      if (!user) return;
      // El selector de usuario no navega: si el rol activo no corresponde a la vista, cambia de ruta.
      const expected = this.isTechnicianView ? 'Técnico' : 'Supervisor';
      if (user.role !== expected) {
        untracked(() => this.router.navigate([this.isTechnicianView ? '/incidentes' : '/mis-asignados']));
        return;
      }
      // Recarga cuando hay (o cambia) el usuario activo: la API exige X-User-Id.
      untracked(() => {
        this.service.loadIncidents();
        this.service.loadKpiIncidents();
      });
    });
  }

  changePage(page: number): void {
    this.service.loadIncidents(page, this.priority());
  }

  /** Un filtro nuevo vuelve a la primera página. */
  changePriority(priority: PriorityFilter): void {
    this.service.loadIncidents(0, priority);
  }

  openIncident(incident: Incident): void {
    this.router.navigate(['/incidentes', incident.id]);
  }
}
