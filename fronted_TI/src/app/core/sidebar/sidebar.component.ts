import { Component, computed, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

export type SidebarRole = 'Supervisor' | 'Técnico';
export type SidebarIcon = 'clipboard-list' | 'plus' | 'settings' | 'user-check';

export interface SidebarUser {
  fullName: string;
  role: SidebarRole;
  area?: string;
}

interface NavItem {
  label: string;
  icon: SidebarIcon;
  route: string;
}

const SUPERVISOR_NAV: NavItem[] = [
  { label: 'Incidentes', icon: 'clipboard-list', route: '/incidentes' },
  { label: 'Registrar incidente', icon: 'plus', route: '/incidentes/nuevo' },
  { label: 'Administración', icon: 'settings', route: '/administracion' },
];

const TECHNICIAN_NAV: NavItem[] = [
  { label: 'Mis asignados', icon: 'user-check', route: '/mis-asignados' },
];

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  currentUser = input.required<SidebarUser>();
  logout = output<void>();

  isSupervisor = computed(() => this.currentUser().role === 'Supervisor');
  sectionLabel = computed(() => (this.isSupervisor() ? 'Gestión global' : 'Mi trabajo'));
  items = computed(() => (this.isSupervisor() ? SUPERVISOR_NAV : TECHNICIAN_NAV));

  initials = computed(() =>
    this.currentUser()
      .fullName.split(' ')
      .filter(Boolean)
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase()
  );

  roleLine = computed(() => {
    const { role, area } = this.currentUser();
    return area ? `${role} · ${area}` : role;
  });
}
