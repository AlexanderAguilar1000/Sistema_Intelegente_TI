import { Component, input, output } from '@angular/core';
import { SimilarCase } from '../../models/incident.model';
import { PriorityBadgeComponent } from '../priority-badge/priority-badge.component';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';

/** Fila-botón de un caso similar con su detalle desplegable (solo lectura). */
@Component({
  selector: 'app-similar-case-item',
  standalone: true,
  imports: [StatusBadgeComponent, PriorityBadgeComponent],
  templateUrl: './similar-case-item.component.html',
  styleUrl: './similar-case-item.component.css'
})
export class SimilarCaseItemComponent {
  case = input.required<SimilarCase>();
  open = input(false);
  toggle = output<void>();
}
