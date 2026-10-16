import { Component, input } from '@angular/core';
import { User } from '../../models/user.model';
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
}
