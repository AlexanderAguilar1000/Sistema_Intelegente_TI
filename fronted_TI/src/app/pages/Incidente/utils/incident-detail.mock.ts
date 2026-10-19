import { IncidentDetail } from '../models/incident.model';
import { toIncidentCode } from './incident-api.util';

type MockDetail = Omit<IncidentDetail, 'code'>;

const MOCK_INCIDENTS: MockDetail[] = [
  {
    id: 412,
    title: 'Fallo de conectividad en servidor de base de datos',
    description: 'El servicio de base de datos no responde a las peticiones de las aplicaciones internas.',
    status: 'En progreso',
    type: 'Red',
    priority: 'Alta',
    area: 'Infraestructura',
    createdAt: '2024-05-22T08:17:00',
    createdBy: 'Ana García',
    assignedToName: 'Carlos Ruiz',
    pendingClassification: false,
    classificationSource: 'Clasificado por IA',
    unindexed: false,
  },
  {
    id: 411,
    title: 'Error 500 al generar reporte de ventas',
    description: 'Al exportar el reporte mensual de ventas la aplicación devuelve un error 500.',
    status: 'Asignado',
    type: 'Bug',
    priority: 'Media',
    area: 'Aplicaciones',
    createdAt: '2024-05-21T16:40:00',
    createdBy: 'Ana García',
    assignedToName: 'María López',
    pendingClassification: false,
    classificationSource: 'Clasificado por IA',
    unindexed: false,
  },
  {
    id: 410,
    title: 'Lentitud en el portal de clientes',
    description: 'El portal tarda más de 20 segundos en cargar durante la mañana.',
    status: 'Registrado',
    type: 'Rendimiento',
    priority: 'Media',
    area: 'Aplicaciones',
    createdAt: '2024-05-21T09:05:00',
    createdBy: 'Ana García',
    pendingClassification: false,
    classificationSource: 'Corregida manualmente',
    unindexed: false,
  },
  {
    id: 409,
    title: 'Usuario bloqueado tras cambio de contraseña',
    description: 'El usuario no puede iniciar sesión después de restablecer su contraseña.',
    status: 'Resuelto',
    type: 'Acceso',
    priority: 'Baja',
    area: 'Soporte a usuario',
    createdAt: '2024-05-20T14:30:00',
    createdBy: 'Ana García',
    assignedToName: 'Pedro Sánchez',
    pendingClassification: false,
    classificationSource: 'Clasificado por IA',
    unindexed: true,
  },
  {
    id: 408,
    title: 'Solicitud de monitor adicional',
    description: 'Se solicita un segundo monitor para el puesto de contabilidad.',
    status: 'Cerrado sin solución',
    type: 'Hardware',
    priority: 'Baja',
    area: 'Soporte a usuario',
    createdAt: '2024-05-20T11:00:00',
    createdBy: 'Ana García',
    assignedToName: 'Pedro Sánchez',
    pendingClassification: false,
    classificationSource: 'Clasificado por IA',
    unindexed: false,
  },
  {
    id: 407,
    title: 'Nueva solicitud sin clasificación',
    description: 'Necesito ayuda con un problema que no sé a qué área pertenece.',
    status: 'Registrado',
    createdAt: '2024-05-20T10:14:00',
    createdBy: 'Ana García',
    pendingClassification: true,
    unindexed: false,
  },
];

/** Datos falsos del detalle mientras no exista el endpoint. */
export function findMockIncident(id: number): IncidentDetail | undefined {
  const found = MOCK_INCIDENTS.find((i) => i.id === id);
  return found ? { ...found, code: toIncidentCode(found.id, found.createdAt) } : undefined;
}
