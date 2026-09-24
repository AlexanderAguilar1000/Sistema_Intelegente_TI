import { Component, HostListener, inject, input, output } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { NewTechnician, TechArea } from '../../models/user.model';
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
  created = output<NewTechnician>();

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
    if (this.form.invalid) return;
    const { fullName, username, area } = this.form.getRawValue();
    this.created.emit({ fullName, username, area: area as TechArea });
    this.form.reset();
  }

  close() {
    this.form.reset();
    this.closed.emit();
  }
}
