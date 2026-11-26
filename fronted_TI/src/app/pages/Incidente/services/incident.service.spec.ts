import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { IncidentApiResponse, IncidentListApiResponse } from '../models/incident.model';
import { IncidentService } from './incident.service';

/** RF-8: el listado se pide paginado al backend. */
describe('IncidentService (listado paginado)', () => {
  let service: IncidentService;
  let http: HttpTestingController;

  const dto: IncidentApiResponse = {
    id: 7,
    title: 'Reporte no carga',
    description: 'Error 500',
    status: 'REGISTERED',
    classificationStatus: 'PENDING',
    type: null,
    priority: null,
    area: null,
    createdBy: 1,
    assignedTo: null,
    assignedToName: null,
    createdAt: '2026-11-20T10:00:00Z',
  };

  const pageOf = (overrides: Partial<IncidentListApiResponse> = {}): IncidentListApiResponse => ({
    incidents: [dto],
    empty: false,
    message: null,
    page: 0,
    size: 20,
    totalElements: 41,
    totalPages: 3,
    ...overrides,
  });

  const expectList = (size: string): TestRequest =>
    http.expectOne((r) => r.method === 'GET' && r.url === '/api/incidents' && r.params.get('size') === size);

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(IncidentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide la primera página de 20 sin filtro y guarda los totales', () => {
    service.loadIncidents();

    const request = expectList('20');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.has('priority')).toBeFalse();
    request.flush(pageOf());

    expect(service.incidents().map((i) => i.id)).toEqual([7]);
    expect(service.page()).toBe(0);
    expect(service.totalElements()).toBe(41);
    expect(service.totalPages()).toBe(3);
    expect(service.loading()).toBeFalse();
  });

  it('envía la página y la prioridad como parámetros de consulta', () => {
    service.loadIncidents(2, 'Alta');

    const request = expectList('20');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('priority')).toBe('HIGH');
    request.flush(pageOf({ page: 2 }));

    expect(service.page()).toBe(2);
    expect(service.priorityFilter()).toBe('Alta');
  });

  it('cancela la petición anterior si se pide otra página antes de que responda', () => {
    service.loadIncidents(0);
    const first = expectList('20');

    service.loadIncidents(1);

    expect(first.cancelled).toBeTrue();
    expectList('20').flush(pageOf({ page: 1 }));
    expect(service.page()).toBe(1);
  });

  it('carga los incidentes de los indicadores sin tocar la página visible', () => {
    service.loadKpiIncidents();

    const request = expectList('100');
    expect(request.request.params.has('priority')).toBeFalse();
    request.flush(pageOf({ size: 100 }));

    expect(service.kpiIncidents().length).toBe(1);
    expect(service.incidents()).toEqual([]);
  });

  it('al registrar un incidente recarga la primera página y los indicadores', () => {
    service.loadIncidents(2, 'Baja');
    expectList('20').flush(pageOf({ page: 2 }));

    service.createIncident({ title: 'Nuevo', description: 'Detalle' }).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === '/api/incidents').flush(dto);

    const list = expectList('20');
    expect(list.request.params.get('page')).toBe('0');
    expect(list.request.params.get('priority')).toBe('LOW');
    list.flush(pageOf());
    expectList('100').flush(pageOf({ size: 100 }));
  });
});
