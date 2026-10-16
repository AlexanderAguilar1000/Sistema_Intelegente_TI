import { Component, computed, input } from '@angular/core';
import { Priority } from '../../models/incident.model';

const PRIORITY_CLASS: Record<Priority, string> = {
  Alta: 'high',
  Media: 'medium',
  Baja: 'low',
};

@Component({
  selector: 'app-priority-badge',
  standalone: true,
  templateUrl: './priority-badge.component.html',
  styleUrl: './priority-badge.component.css'
})
export class PriorityBadgeComponent {
  priority = input<Priority | undefined>();
  cssClass = computed(() => {
    const priority = this.priority();
    return priority ? PRIORITY_CLASS[priority] : '';
  });
}
