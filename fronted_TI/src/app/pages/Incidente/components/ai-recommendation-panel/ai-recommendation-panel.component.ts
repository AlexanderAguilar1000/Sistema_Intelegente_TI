import { Component, input } from '@angular/core';

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
  templateUrl: './ai-recommendation-panel.component.html',
  styleUrl: './ai-recommendation-panel.component.css'
})
export class AiRecommendationPanelComponent {
  /** false = bloqueado (antes de pulsar "Analizar con IA"). */
  open = input(false);
  steps = MOCK_STEPS;
}
