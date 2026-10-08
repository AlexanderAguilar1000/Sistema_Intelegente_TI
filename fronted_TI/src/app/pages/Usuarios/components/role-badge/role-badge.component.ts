import { Component, input } from '@angular/core';
import { UserRole } from '../../models/user.model';

@Component({
  selector: 'app-role-badge',
  standalone: true,
  template: `<span class="badge" [class.supervisor]="role() === 'Supervisor'" [class.tech]="role() === 'Técnico'">{{ role() }}</span>`,
  styles: `
    .badge { display: inline-flex; align-items: center; height: 20px; padding: 0 8px; border-radius: 999px; font-size: 11px; font-weight: 500; }
    .supervisor { background: #eae3ff; color: #6b43b3; }
    .tech { background: #e1f5f1; color: #187c72; }
  `
})
export class RoleBadgeComponent {
  role = input.required<UserRole>();
}
