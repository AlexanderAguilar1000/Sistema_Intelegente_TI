import { Component, input, signal } from '@angular/core';
import { SimilarCase } from '../../models/incident.model';
import { SimilarCaseItemComponent } from '../similar-case-item/similar-case-item.component';

interface DiagnosticStep {
  title: string;
  description: string;
}

/** Pasos de ejemplo hasta conectar el servicio de IA (ver doc 7.11). */
const MOCK_STEPS: DiagnosticStep[] = [
  'Verificar estado del servicio y del servidor',
  'Comprobar conectividad y puertos',
  'Revisar logs de aplicación y balanceador',
].map((title) => ({
  title,
  description: 'Valida la conectividad, revisa recursos y contrasta el comportamiento reciente.',
}));

@Component({
  selector: 'app-ai-recommendation-panel',
  standalone: true,
  imports: [SimilarCaseItemComponent],
  templateUrl: './ai-recommendation-panel.component.html',
  styleUrl: './ai-recommendation-panel.component.css'
})
export class AiRecommendationPanelComponent {
  /** false = bloqueado (antes de pulsar "Analizar con IA"). */
  open = input(false);
  cases = input<SimilarCase[]>([]);
  steps = MOCK_STEPS;

  /** Código del caso desplegado; solo uno a la vez. */
  openCase = signal<string | null>(null);

  toggle(code: string): void {
    this.openCase.update((current) => (current === code ? null : code));
  }
}
