import { Incident, IncidentKpi, IncidentStatus, Priority, PriorityFilter, StatusFilter } from '../models/incident.model';

export const PRIORITIES: Priority[] = ['Alta', 'Media', 'Baja'];
export const STATUSES: IncidentStatus[] = ['Registrado', 'Asignado', 'En progreso', 'Resuelto', 'Cerrado sin solución'];

const WEEK_MS = 7 * 24 * 60 * 60 * 1000;

export const pad2 = (n: number): string => String(n).padStart(2, '0');

/** "Octubre 2026" para el mes en curso. */
export function currentPeriod(now: Date = new Date()): string {
  const month = now.toLocaleDateString('es', { month: 'long' });
  return `${month} ${now.getFullYear()}`;
}

/** Estados finales: el incidente ya no está abierto. */
const isClosed = (status: IncidentStatus): boolean => status === 'Resuelto' || status === 'Cerrado sin solución';

function createdThisMonth(incident: Incident, now: Date): boolean {
  const date = new Date(incident.createdAt);
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
}

/**
 * KPIs calculados con los incidentes cargados. El listado no trae la fecha de resolución, así que
 * "Resueltos este mes" cuenta los resueltos creados en el mes en curso.
 */
export function buildKpis(incidents: Incident[], now: Date = new Date()): IncidentKpi[] {
  const open = incidents.filter((i) => !isClosed(i.status));
  const openThisWeek = open.filter((i) => now.getTime() - new Date(i.createdAt).getTime() <= WEEK_MS).length;
  const inProgress = incidents.filter((i) => i.status === 'En progreso');
  const highPriority = inProgress.filter((i) => i.priority === 'Alta').length;
  const resolved = incidents.filter((i) => i.status === 'Resuelto' && createdThisMonth(i, now)).length;
  const pending = incidents.filter((i) => i.pendingClassification).length;

  return [
    { label: 'Incidentes abiertos', value: pad2(open.length), helper: `+${openThisWeek} esta semana`, icon: 'clipboard-list', tone: 'blue' },
    { label: 'En progreso', value: pad2(inProgress.length), helper: `${highPriority} de prioridad alta`, icon: 'activity', tone: 'amber' },
    { label: 'Resueltos este mes', value: pad2(resolved), helper: 'Del mes en curso', icon: 'shield-check', tone: 'green' },
    { label: 'Pendientes de clasificación', value: pad2(pending), helper: 'Requieren revisión', icon: 'alert-circle', tone: 'red' },
  ];
}

/** Busca por título o código (sin distinguir mayúsculas) y filtra por estado y prioridad. */
export function filterIncidents(
  incidents: Incident[],
  search: string,
  status: StatusFilter,
  priority: PriorityFilter,
): Incident[] {
  const term = search.trim().toLowerCase();
  return incidents.filter(
    (i) =>
      (!term || i.title.toLowerCase().includes(term) || i.code.toLowerCase().includes(term)) &&
      (priority === 'all' || i.priority === priority) &&
      (status === 'all' || i.status === status),
  );
}
