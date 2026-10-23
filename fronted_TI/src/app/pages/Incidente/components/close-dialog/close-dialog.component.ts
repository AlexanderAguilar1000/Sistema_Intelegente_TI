import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IncidentDetail } from '../../models/incident.model';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-close-dialog',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './close-dialog.component.html',
  styleUrl: './close-dialog.component.css'
})
export class CloseDialogComponent {
  private incidents = inject(IncidentService);

  open = input(false);
  incident = input.required<IncidentDetail>();
  closed = output<void>();
  /** Detalle devuelto por el backend tras cerrar sin solución. */
  saved = output<IncidentDetail>();

  reason = signal('');
  saving = signal(false);
  submitError = signal<string | null>(null);

  constructor() {
    // Cada vez que se abre, parte con el formulario limpio.
    effect(() => {
      if (!this.open()) return;
      this.reason.set('');
      this.submitError.set(null);
    });
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.open()) this.close();
  }

  submit() {
    const reason = this.reason().trim();
    if (!reason || this.saving()) return;
    this.saving.set(true);
    this.submitError.set(null);
    this.incidents.closeWithoutSolution(this.incident().id, reason).subscribe({
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
    if (err.status === 403) return err.error?.message ?? 'Solo el técnico asignado puede cerrar el incidente.';
    if (err.status === 404) return 'No se encontró el incidente.';
    return err.error?.message ?? 'No se pudo cerrar el incidente.';
  }
}
