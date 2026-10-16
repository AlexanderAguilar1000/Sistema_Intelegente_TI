import { Component, effect, inject, signal, untracked } from '@angular/core';
import { AuthService } from '../Usuarios/services/auth.service';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { TeamRowComponent } from './components/team-row/team-row.component';
import { UserCreateDialogComponent } from './components/user-create-dialog/user-create-dialog.component';
import { UserService } from './services/user.service';

@Component({
  selector: 'app-tecnicos',
  standalone: true,
  imports: [TeamRowComponent, EmptyStateComponent, UserCreateDialogComponent],
  templateUrl: './tecnicos.component.html',
  styleUrl: './tecnicos.component.css'
})
export class TecnicosComponent {
  private users = inject(UserService);
  private auth = inject(AuthService);

  technicians = this.users.technicians;
  loading = this.users.loading;
  error = this.users.error;
  createOpen = signal(false);

  constructor() {
    // Recarga cuando hay (o cambia) el usuario activo: la API exige X-User-Id.
    effect(() => {
      if (this.auth.activeUser()) untracked(() => this.users.loadTechnicians());
    });
  }

  onCreated() {
    this.createOpen.set(false);
  }
}
