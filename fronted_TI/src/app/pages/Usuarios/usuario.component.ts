import { Component, effect, inject, model } from '@angular/core';
import { Router } from '@angular/router';
import { UserSelectorDialogComponent } from './components/user-selector-dialog/user-selector-dialog.component';
import { AppUser } from './models/user.model';
import { AuthService } from './services/auth.service';
import { UsuariosService } from './services/user.service';
import { homeRouteFor } from './utils/user.util';

@Component({
  selector: 'app-usuario',
  standalone: true,
  imports: [UserSelectorDialogComponent],
  templateUrl: './usuario.component.html',
  styleUrl: './usuario.component.css'
})
export class UsuarioComponent {
  private usuarios = inject(UsuariosService);
  private auth = inject(AuthService);
  private router = inject(Router);

  /** Controla si el selector está abierto (two-way: [(open)]). */
  open = model(false);

  users = this.usuarios.users;
  loading = this.usuarios.loading;
  error = this.usuarios.error;
  activeUser = this.auth.activeUser;

  constructor() {
    // Cada vez que se abre, se refresca la lista (incluye técnicos recién creados)
    effect(() => {
      if (this.open()) this.usuarios.load();
    });
  }

  onConfirmed(user: AppUser) {
    this.auth.switchUser(user);
    this.open.set(false);
    this.router.navigateByUrl(homeRouteFor(user)).catch(() => {});
  }
}
