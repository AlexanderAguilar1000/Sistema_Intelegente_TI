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
  /** Muestra el badge de rol (el selector de usuario lo usa; el diálogo de asignar no). */
  showRole = input(true);
  /** Avatar grande (44 px) o normal (32 px). */
  large = input(true);
  /** Texto secundario; por defecto "Rol · Área". */
  subtitleText = input<string>();
  select = output<void>();

  subtitle = computed(() => this.subtitleText() ?? roleLine(this.user()));
}
