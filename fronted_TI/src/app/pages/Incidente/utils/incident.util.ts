import { Incident, IncidentKpi, IncidentStatus, Priority, PriorityFilter, StatusFilter } from '../models/incident.model';

export const PRIORITIES: Priority[] = ['Alta', 'Media', 'Baja'];
export const STATUSES: IncidentStatus[] = ['Registrado', 'Asignado', 'En progreso', 'Resuelto', 'Cerrado sin solución'];

/** Período fijo de la maqueta: los datos de prueba son de mayo de 2024. */
export const KPI_PERIOD = 'Mayo 2024';
const KPI_PERIOD_YEAR = 2024;
const KPI_PERIOD_MONTH = 4; // mayo (base 0)

export const pad2 = (n: number): string => String(n).padStart(2, '0');

/** Estados finales: el incidente ya no está abierto. */
const isClosed = (status: IncidentStatus): boolean => status === 'Resuelto' || status === 'Cerrado sin solución';

function resolvedInPeriod(incident: Incident): boolean {
  if (incident.status !== 'Resuelto' || !incident.resolvedAt) return false;
  const date = new Date(incident.resolvedAt);
  return date.getFullYear() === KPI_PERIOD_YEAR && date.getMonth() === KPI_PERIOD_MONTH;
}

export function buildKpis(incidents: Incident[]): IncidentKpi[] {
  const open = incidents.filter((i) => !isClosed(i.status)).length;
  const inProgress = incidents.filter((i) => i.status === 'En progreso');
  const highPriority = inProgress.filter((i) => i.priority === 'Alta').length;
  const resolved = incidents.filter(resolvedInPeriod).length;
  const pending = incidents.filter((i) => i.pendingClassification).length;

  return [
    { label: 'Incidentes abiertos', value: pad2(open), helper: '+3 esta semana', icon: 'clipboard-list', tone: 'blue' },
    { label: 'En progreso', value: pad2(inProgress.length), helper: `${highPriority} de prioridad alta`, icon: 'activity', tone: 'amber' },
    { label: 'Resueltos este mes', value: pad2(resolved), helper: '+12% vs. mes anterior', icon: 'shield-check', tone: 'green' },
    { label: 'Pendientes de clasificación', value: pad2(pending), helper: 'Requieren revisión', icon: 'alert-circle', tone: 'red' },
  ];
}

/** Busca por título o ID (sin distinguir mayúsculas) y filtra por estado y prioridad. */
export function filterIncidents(
  incidents: Incident[],
  search: string,
  status: StatusFilter,
  priority: PriorityFilter,
): Incident[] {
  const term = search.trim().toLowerCase();
  return incidents.filter(
    (i) =>
      (!term || i.title.toLowerCase().includes(term) || i.id.toLowerCase().includes(term)) &&
      (priority === 'all' || i.priority === priority) &&
      (status === 'all' || i.status === status),
  );
}

/** Más recientes primero. */
export const sortByCreatedDesc = (incidents: Incident[]): Incident[] =>
  [...incidents].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
