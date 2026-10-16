import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TechArea } from '../../models/user.model';
import { UserService } from '../../services/user.service';
import { AREAS } from '../../utils/areas.util';
import { USERNAME_PATTERN } from '../../utils/user.util';

@Component({
  selector: 'app-user-create-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './user-create-dialog.component.html',
  styleUrl: './user-create-dialog.component.css'
})
export class UserCreateDialogComponent {
  private users = inject(UserService);

  open = input(false);
  closed = output<void>();
  created = output<void>();
  saving = signal(false);
  submitError = signal<string | null>(null);

  readonly areas = AREAS;

  form = new FormGroup({
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    username: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.pattern(USERNAME_PATTERN),
        (c: AbstractControl): ValidationErrors | null =>
          this.users.usernameExists(c.value ?? '') ? { taken: true } : null,
      ],
    }),
    area: new FormControl<TechArea | ''>('', { nonNullable: true, validators: [Validators.required] }),
  });

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.open()) this.close();
  }

  showUsernameError() {
    const c = this.form.controls.username;
    return c.touched && c.invalid && !c.hasError('required');
  }

  submit() {
    if (this.form.invalid || this.saving()) return;
    const { fullName, username, area } = this.form.getRawValue();
    this.saving.set(true);
    this.submitError.set(null);
    this.users.createTechnician({ fullName, username, area: area as TechArea }).subscribe({
      next: () => {
        this.saving.set(false);
        this.form.reset();
        this.created.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.submitError.set(err.error?.message ?? 'No se pudo crear el técnico.');
      },
    });
  }

  close() {
    this.submitError.set(null);
    this.form.reset();
    this.closed.emit();
  }
}
