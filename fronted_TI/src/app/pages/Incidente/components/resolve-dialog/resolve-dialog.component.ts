import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IncidentDetail } from '../../models/incident.model';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-resolve-dialog',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './resolve-dialog.component.html',
  styleUrl: './resolve-dialog.component.css'
})
export class ResolveDialogComponent {
  private incidents = inject(IncidentService);

  open = input(false);
  incident = input.required<IncidentDetail>();
  closed = output<void>();
  /** Detalle devuelto por el backend tras resolver. */
  saved = output<IncidentDetail>();

  solution = signal('');
  saving = signal(false);
  submitError = signal<string | null>(null);

  constructor() {
    // Cada vez que se abre, parte con el formulario limpio.
    effect(() => {
      if (!this.open()) return;
      this.solution.set('');
      this.submitError.set(null);
    });
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.open()) this.close();
  }

  submit() {
    const solution = this.solution().trim();
    if (!solution || this.saving()) return;
    this.saving.set(true);
    this.submitError.set(null);
    this.incidents.resolveIncident(this.incident().id, solution).subscribe({
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
    if (err.status === 403) return err.error?.message ?? 'Solo el técnico asignado puede resolver el incidente.';
    if (err.status === 404) return 'No se encontró el incidente.';
    return err.error?.message ?? 'No se pudo registrar la solución.';
  }
}
