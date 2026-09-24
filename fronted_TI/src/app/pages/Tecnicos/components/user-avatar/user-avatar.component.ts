import { Component, computed, input } from '@angular/core';
import { getInitials } from '../../utils/user.util';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  templateUrl: './user-avatar.component.html',
  styleUrl: './user-avatar.component.css'
})
export class UserAvatarComponent {
  name = input<string>('Administrador');
  large = input(false);
  initials = computed(() => getInitials(this.name()));
}
