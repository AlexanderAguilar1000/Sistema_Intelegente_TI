import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { Incident, IncidentStatus, IncidentUser, Priority, PriorityFilter, StatusFilter } from '../../models/incident.model';
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
  users = input<IncidentUser[]>([]);
  open = output<Incident>();

  readonly statuses: IncidentStatus[] = STATUSES;
  readonly priorities: Priority[] = PRIORITIES;

  search = signal('');
  status = signal<StatusFilter>('all');
  priority = signal<PriorityFilter>('all');

  filtered = computed(() => filterIncidents(this.incidents(), this.search(), this.status(), this.priority()));

  private fullNameByUsername = computed(() => new Map(this.users().map((u) => [u.username, u.fullName])));

  technicianName(username?: string): string {
    return (username && this.fullNameByUsername().get(username)) || '—';
  }

  clearFilters(): void {
    this.search.set('');
    this.status.set('all');
    this.priority.set('all');
  }

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  onStatus(event: Event): void {
    this.status.set((event.target as HTMLSelectElement).value as StatusFilter);
  }

  onPriority(event: Event): void {
    this.priority.set((event.target as HTMLSelectElement).value as PriorityFilter);
  }

  onRowKey(event: Event, incident: Incident): void {
    event.preventDefault();
    this.open.emit(incident);
  }
}
