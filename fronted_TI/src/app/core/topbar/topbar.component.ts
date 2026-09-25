import { Component, computed, input, output } from '@angular/core';

export type TopbarRole = 'Supervisor' | 'Técnico';

export interface TopbarUser {
  fullName: string;
  role: TopbarRole;
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent {
  currentUser = input.required<TopbarUser>();
  hasNotifications = input(true);

  chooseUser = output<void>();
  menuClick = output<void>();

  section = computed(() => (this.currentUser().role === 'Supervisor' ? 'Incidentes' : 'Mis asignados'));

  initials = computed(() =>
    this.currentUser()
      .fullName.split(' ')
      .filter(Boolean)
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase()
  );
}
