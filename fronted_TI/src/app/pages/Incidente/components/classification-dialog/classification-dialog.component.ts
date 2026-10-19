import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  INCIDENT_TYPES,
  IncidentDetail,
  IncidentType,
  PRIORITIES,
  Priority,
  TECH_AREAS,
  TechArea,
} from '../../models/incident.model';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-classification-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './classification-dialog.component.html',
  styleUrl: './classification-dialog.component.css'
})
export class ClassificationDialogComponent {
  private incidents = inject(IncidentService);

  open = input(false);
  incident = input.required<IncidentDetail>();
  /** Técnico asignado (nombre y área), para avisar si el nuevo área lo excluye. */
  assignedTechName = input<string>();
  assignedTechArea = input<string>();
  closed = output<void>();
  /** Detalle devuelto por el backend tras guardar. */
  saved = output<IncidentDetail>();

  readonly types = INCIDENT_TYPES;
  readonly priorities = PRIORITIES;
  readonly areas = TECH_AREAS;

  saving = signal(false);
  submitError = signal<string | null>(null);

  // Cadena vacía = sin seleccionar (incidente pendiente de clasificación).
  form = new FormGroup({
    type: new FormControl<IncidentType | ''>('', { nonNullable: true, validators: [Validators.required] }),
    priority: new FormControl<Priority | ''>('', { nonNullable: true, validators: [Validators.required] }),
    area: new FormControl<TechArea | ''>('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    // Cada vez que se abre, precarga los valores actuales del incidente.
    effect(() => {
      if (!this.open()) return;
      const inc = this.incident();
      this.submitError.set(null);
      this.form.reset({ type: inc.type ?? '', priority: inc.priority ?? '', area: inc.area ?? '' });
    });
  }

  /** Solo informativo: la regla real (retirar asignación y volver a Registrado) la aplica el backend. */
  willUnassign(): boolean {
    const techArea = this.assignedTechArea();
    const newArea = this.form.controls.area.value;
    return !!this.incident().assignedTo && !!techArea && !!newArea && techArea !== newArea;
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.open()) this.close();
  }

  submit() {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { type, priority, area } = this.form.getRawValue();
    this.saving.set(true);
    this.submitError.set(null);
    this.incidents
      .correctClassification(this.incident().id, {
        type: type as IncidentType,
        priority: priority as Priority,
        area: area as TechArea,
      })
      .subscribe({
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
    if (err.status === 403) return err.error?.message ?? 'Solo un supervisor puede corregir la clasificación.';
    if (err.status === 404) return 'No se encontró el incidente.';
    return err.error?.message ?? 'No se pudo guardar la clasificación.';
  }
}
