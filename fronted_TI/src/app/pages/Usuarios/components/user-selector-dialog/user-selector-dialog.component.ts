import { Component, effect, input, output, signal } from '@angular/core';
import { AppUser } from '../../models/user.model';
import { UserOptionComponent } from '../user-option/user-option.component';

@Component({
  selector: 'app-user-selector-dialog',
  standalone: true,
  imports: [UserOptionComponent],
  templateUrl: './user-selector-dialog.component.html',
  styleUrl: './user-selector-dialog.component.css',
  host: { '(document:keydown.escape)': 'close()' }
})
export class UserSelectorDialogComponent {
  open = input(false);
  users = input<AppUser[]>([]);
  activeUser = input<AppUser | null>(null);
  loading = input(false);
  error = input<string | null>(null);

  confirmed = output<AppUser>();
  closed = output<void>();

  selectedId = signal<number | null>(null);

  constructor() {
    // Al abrir, se preselecciona el usuario activo
    effect(() => {
      if (this.open()) this.selectedId.set(this.activeUser()?.id ?? null);
    });
  }

  close() {
    if (this.open()) this.closed.emit();
  }

  confirm() {
    const user = this.users().find((u) => u.id === this.selectedId());
    if (user) this.confirmed.emit(user);
  }
}
