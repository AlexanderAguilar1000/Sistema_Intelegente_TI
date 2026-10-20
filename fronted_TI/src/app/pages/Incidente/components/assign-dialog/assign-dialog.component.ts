import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, effect, inject, input, output, signal } from '@angular/core';
import { UserOptionComponent } from '../../../Usuarios/components/user-option/user-option.component';
import { UsuariosService } from '../../../Usuarios/services/user.service';
import { IncidentDetail } from '../../models/incident.model';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-assign-dialog',
  standalone: true,
  imports: [UserOptionComponent],
  templateUrl: './assign-dialog.component.html',
  styleUrl: './assign-dialog.component.css'
})
export class AssignDialogComponent {
  private incidents = inject(IncidentService);
  private usuarios = inject(UsuariosService);

  open = input(false);
  incident = input.required<IncidentDetail>();
  closed = output<void>();
  /** Detalle devuelto por el backend tras asignar. */
  saved = output<IncidentDetail>();

  /** Solo técnicos del área responsable del incidente. */
  candidates = computed(() => {
    const area = this.incident().area;
    if (!area) return [];
    return this.usuarios.users().filter((u) => u.role === 'Técnico' && u.area === area);
  });

  title = computed(() => (this.incident().assignedTo ? 'Reasignar incidente' : 'Asignar incidente'));

  selected = signal<number | null>(null);
  saving = signal(false);
  submitError = signal<string | null>(null);

  constructor() {
    // Cada vez que se abre, preselecciona al técnico actual (si lo hay).
    effect(() => {
      if (!this.open()) return;
      this.submitError.set(null);
      this.selected.set(this.incident().assignedTo ?? null);
    });
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.open()) this.close();
  }

  submit() {
    const technician = this.candidates().find((u) => u.id === this.selected());
    if (!technician || this.saving()) return;
    this.saving.set(true);
    this.submitError.set(null);
    this.incidents.assignIncident(this.incident().id, technician.id, technician.fullName).subscribe({
      next: (detail) => {
        this.saving.set(false);
        this.saved.emit(detail);
        this.closed.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.submitError.set(this.errorMessage(err));
      },
    });
  }

  close() {
    if (this.saving()) return;
    this.closed.emit();
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 403) return err.error?.message ?? 'Solo un supervisor puede asignar incidentes.';
    if (err.status === 404) return 'No se encontró el incidente o el técnico.';
    return err.error?.message ?? 'No se pudo asignar el incidente.';
  }
}
