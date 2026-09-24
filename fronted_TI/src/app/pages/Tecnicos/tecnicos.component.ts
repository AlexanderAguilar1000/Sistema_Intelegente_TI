import { Component, inject, signal } from '@angular/core';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { TeamRowComponent } from './components/team-row/team-row.component';
import { UserCreateDialogComponent } from './components/user-create-dialog/user-create-dialog.component';
import { NewTechnician } from './models/user.model';
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

  technicians = this.users.technicians;
  createOpen = signal(false);

  onCreated(data: NewTechnician) {
    this.users.addTechnician(data);
    this.createOpen.set(false);
  }
}
