import { Component, input } from '@angular/core';
import { IncidentAnalysis } from '../../models/incident.model';

@Component({
  selector: 'app-ai-recommendation-panel',
  standalone: true,
  templateUrl: './ai-recommendation-panel.component.html',
  styleUrl: './ai-recommendation-panel.component.css'
})
export class AiRecommendationPanelComponent {
  /** false = bloqueado (antes de pulsar "Analizar con IA"). */
  open = input(false);
  loading = input(false);
  error = input<string | null>(null);
  analysis = input<IncidentAnalysis | null>(null);

  /** Score 0-1 del backend a porcentaje entero. */
  percent(score: number): number {
    return Math.round(score * 100);
  }
}
