import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Incident } from '../../models/incident.model';
import { IncidentService } from '../../services/incident.service';

@Component({
  selector: 'app-incident-create-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './incident-create-dialog.component.html',
  styleUrl: './incident-create-dialog.component.css'
})
export class IncidentCreateDialogComponent {
  private incidents = inject(IncidentService);

  open = input(false);
  closed = output<void>();
  created = output<void>();

  saving = signal(false);
  submitError = signal<string | null>(null);
  /** Incidente recién creado (muestra la tarjeta de éxito). */
  createdIncident = signal<Incident | null>(null);

  form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.open()) this.close();
  }

  showError(name: 'title' | 'description') {
    const c = this.form.controls[name];
    return c.touched && c.invalid;
  }

  submit() {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.submitError.set(null);
    this.incidents.createIncident(this.form.getRawValue()).subscribe({
      next: (incident) => {
        this.saving.set(false);
        this.form.reset();
        this.createdIncident.set(incident);
        this.created.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.submitError.set(err.error?.message ?? 'No se pudo registrar el incidente.');
      },
    });
  }

  close() {
    this.submitError.set(null);
    this.createdIncident.set(null);
    this.form.reset();
    this.closed.emit();
  }
}
