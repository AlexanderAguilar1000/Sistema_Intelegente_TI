import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { Incident, IncidentStatus, Priority, PriorityFilter, StatusFilter } from '../../models/incident.model';
import { PRIORITIES, STATUSES, filterIncidents } from '../../utils/incident.util';
import { PriorityBadgeComponent } from '../priority-badge/priority-badge.component';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';

@Component({
  selector: 'app-incident-table',
  standalone: true,
  imports: [DatePipe, StatusBadgeComponent, PriorityBadgeComponent],
  templateUrl: './incident-table.component.html',
  styleUrl: './incident-table.component.css'
})
export class IncidentTableComponent {
  incidents = input.required<Incident[]>();
  loading = input(false);
  error = input<string | null>(null);
  heading = input('Todos los incidentes');
  subheading = input('Consulta y gestiona el catálogo global de incidencias.');
  showStatusFilter = input(true);
  /** Página visible (empieza en 0) y totales que devuelve el backend. */
  page = input(0);
  totalPages = input(0);
  totalElements = input(0);
  /** El filtro de prioridad lo aplica el backend: la tabla solo lo muestra y pide cambiarlo. */
  priority = input<PriorityFilter>('all');
  open = output<Incident>();
  pageChange = output<number>();
  priorityChange = output<PriorityFilter>();

  readonly statuses: IncidentStatus[] = STATUSES;
  readonly priorities: Priority[] = PRIORITIES;

  search = signal('');
  status = signal<StatusFilter>('all');

  filtered = computed(() => filterIncidents(this.incidents(), this.search(), this.status()));
  /** Búsqueda o estado activos: solo revisan la página visible (pendiente de T-47). */
  pageFiltersActive = computed(() => this.search().trim() !== '' || this.status() !== 'all');

  clearFilters(): void {
    this.search.set('');
    this.status.set('all');
    if (this.priority() !== 'all') this.priorityChange.emit('all');
  }

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  onStatus(event: Event): void {
    this.status.set((event.target as HTMLSelectElement).value as StatusFilter);
  }

  onPriority(event: Event): void {
    this.priorityChange.emit((event.target as HTMLSelectElement).value as PriorityFilter);
  }

  onRowKey(event: Event, incident: Incident): void {
    event.preventDefault();
    this.open.emit(incident);
  }
}
