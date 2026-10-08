import { Component, computed, input, output } from '@angular/core';
import { UserAvatarComponent } from '../../../Tecnicos/components/user-avatar/user-avatar.component';
import { AppUser } from '../../models/user.model';
import { roleLine } from '../../utils/user.util';
import { RoleBadgeComponent } from '../role-badge/role-badge.component';

@Component({
  selector: 'app-user-option',
  standalone: true,
  imports: [UserAvatarComponent, RoleBadgeComponent],
  templateUrl: './user-option.component.html',
  styleUrl: './user-option.component.css'
})
export class UserOptionComponent {
  user = input.required<AppUser>();
  selected = input(false);
  select = output<void>();

  subtitle = computed(() => roleLine(this.user()));
}
