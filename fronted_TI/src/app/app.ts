import { Component, computed, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent, SidebarUser } from './core/sidebar/sidebar.component';
import { TopbarComponent } from './core/topbar/topbar.component';
import { UsuarioComponent } from './pages/Usuarios/usuario.component';
import { AuthService } from './pages/Usuarios/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, UsuarioComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'Alexander';

  private auth = inject(AuthService);

  // Sin usuario activo se abre el selector (simula el inicio de sesión)
  selectorOpen = signal(this.auth.activeUser() === null);

  currentUser = computed<SidebarUser>(
    () => this.auth.activeUser() ?? { fullName: 'Sin usuario', role: 'Supervisor' }
  );
}
