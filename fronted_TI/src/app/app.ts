import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent, SidebarUser } from './core/sidebar/sidebar.component';
import { TopbarComponent } from './core/topbar/topbar.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'Alexander';

  // Usuario de prueba hasta que exista AuthService
  currentUser: SidebarUser = { fullName: 'Ana García', role: 'Supervisor' };
}
