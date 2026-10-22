import { Incident, SimilarCase } from '../models/incident.model';

/** Base de conocimiento simulada hasta conectar el servicio de IA. */
const SIMILAR_CASES_KB: SimilarCase[] = [
  {
    code: 'INC-2024-0388', title: 'Fallo de acceso a APP-SRV-01', similarity: 92, type: 'Red', priority: 'Alta', area: 'Infraestructura', status: 'Resuelto',
    description: 'Las aplicaciones internas perdían la conexión con el servidor APP-SRV-01 de forma intermitente.',
    solution: 'Se reinició el servicio de red, se corrigió la regla del firewall que bloqueaba el puerto 1433 y se validó la conectividad con el balanceador.',
    resolvedBy: 'Carlos Ruiz', resolvedAt: '14/05/2024 17:40',
  },
  {
    code: 'INC-2024-0371', title: 'Caída del servicio de base de datos tras mantenimiento', similarity: 84, type: 'Configuración', priority: 'Alta', area: 'Infraestructura', status: 'Resuelto',
    description: 'Después de la ventana de mantenimiento, el servicio de base de datos dejó de aceptar conexiones.',
    solution: 'Se restauró la configuración previa del listener y se añadió una comprobación de conectividad al procedimiento de mantenimiento.',
    resolvedBy: 'Pedro Sánchez', resolvedAt: '03/05/2024 12:15',
  },
  {
    code: 'INC-2024-0352', title: 'Timeouts en aplicación de reportes', similarity: 76, type: 'Rendimiento', priority: 'Media', area: 'Aplicaciones', status: 'Resuelto',
    description: 'Los reportes mensuales superaban el tiempo máximo de espera y no llegaban a mostrarse.',
    solution: 'Se optimizó la consulta principal, se creó un índice sobre la fecha de venta y se amplió el timeout del pool de conexiones.',
    resolvedBy: 'María López', resolvedAt: '26/04/2024 10:05',
  },
  {
    code: 'INC-2024-0339', title: 'Accesos rechazados al portal corporativo', similarity: 68, type: 'Acceso', priority: 'Media', area: 'Soporte a usuario', status: 'Resuelto',
    description: 'Varios usuarios recibían error de credenciales al entrar al portal tras un cambio de contraseña.',
    solution: 'Se sincronizó el directorio de usuarios y se limpió la caché de sesiones del portal.',
    resolvedBy: 'Pedro Sánchez', resolvedAt: '18/04/2024 09:30',
  },
];

/** Hasta 3 casos: primero los del área del incidente, luego por mayor similitud; nunca el propio incidente. */
export function getSimilarCases(incident: Pick<Incident, 'code' | 'area'>): SimilarCase[] {
  return SIMILAR_CASES_KB.filter((c) => c.code !== incident.code)
    .sort((a, b) => Number(b.area === incident.area) - Number(a.area === incident.area) || b.similarity - a.similarity)
    .slice(0, 3);
}
