import { Component, computed, input } from '@angular/core';
import { User } from '../../models/user.model';
import { toCorporateEmail } from '../../utils/user.util';
import { UserAvatarComponent } from '../user-avatar/user-avatar.component';

@Component({
  selector: 'app-team-row',
  standalone: true,
  imports: [UserAvatarComponent],
  templateUrl: './team-row.component.html',
  styleUrl: './team-row.component.css'
})
export class TeamRowComponent {
  user = input.required<User>();
  email = computed(() => toCorporateEmail(this.user().username));
}
