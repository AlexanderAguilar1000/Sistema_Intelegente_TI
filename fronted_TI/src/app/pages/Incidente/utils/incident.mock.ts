import { Incident } from '../models/incident.model';

const DAY_MS = 24 * 60 * 60 * 1000;
const daysAgo = (days: number): string => new Date(Date.now() - days * DAY_MS).toISOString();

type MockIncident = Incident & { assignedToName: string };

/** Datos de prueba mientras la vista de técnico no se conecta al backend. */
const MOCK_INCIDENTS: MockIncident[] = [
  {
    id: 412,
    code: 'INC-2024-0412',
    title: 'Fallo de conectividad en servidor de base de datos',
    description: 'El servidor de base de datos pierde conexión de forma intermitente.',
    status: 'En progreso',
    type: 'Red',
    priority: 'Alta',
    area: 'Infraestructura',
    createdAt: daysAgo(1),
    assignedToName: 'Carlos Ruiz',
    pendingClassification: false,
  },
  {
    id: 411,
    code: 'INC-2024-0411',
    title: 'Error al generar facturas en el módulo de ventas',
    description: 'El módulo de facturación devuelve un error 500 al emitir comprobantes.',
    status: 'Asignado',
    type: 'Bug',
    priority: 'Media',
    area: 'Aplicaciones',
    createdAt: daysAgo(2),
    assignedToName: 'María López',
    pendingClassification: false,
  },
  {
    id: 409,
    code: 'INC-2024-0409',
    title: 'Lentitud en la carga de reportes mensuales',
    description: 'Los reportes tardan más de un minuto en abrir.',
    status: 'Resuelto',
    type: 'Rendimiento',
    priority: 'Baja',
    area: 'Aplicaciones',
    createdAt: daysAgo(3),
    assignedToName: 'María López',
    pendingClassification: false,
  },
];

/** Incidentes de prueba asignados al técnico indicado (Pedro Sánchez no tiene ninguno). */
export function mockIncidentsFor(fullName: string): Incident[] {
  return MOCK_INCIDENTS.filter((i) => i.assignedToName === fullName);
}
