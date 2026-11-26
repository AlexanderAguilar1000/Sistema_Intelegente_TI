import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Incident, PriorityFilter } from '../../models/incident.model';
import { IncidentTableComponent } from './incident-table.component';

/** RF-8: paginador y filtro de prioridad de la tabla; quien pide los datos es la página. */
describe('IncidentTableComponent (paginación)', () => {
  let fixture: ComponentFixture<IncidentTableComponent>;
  let element: HTMLElement;
  let pages: number[];
  let priorities: PriorityFilter[];

  const incident: Incident = {
    id: 7,
    code: 'INC-2026-0007',
    title: 'Reporte no carga',
    description: 'Error 500',
    status: 'Registrado',
    createdAt: '2026-11-20T10:00:00Z',
    pendingClassification: true,
  };

  const render = (incidents: Incident[], page: number, totalPages: number, totalElements: number): void => {
    fixture.componentRef.setInput('incidents', incidents);
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('totalPages', totalPages);
    fixture.componentRef.setInput('totalElements', totalElements);
    fixture.detectChanges();
  };

  const button = (label: string): HTMLButtonElement =>
    element.querySelector<HTMLButtonElement>(`button[aria-label="${label}"]`)!;

  beforeEach(() => {
    fixture = TestBed.createComponent(IncidentTableComponent);
    element = fixture.nativeElement as HTMLElement;
    pages = [];
    priorities = [];
    fixture.componentInstance.pageChange.subscribe((page) => pages.push(page));
    fixture.componentInstance.priorityChange.subscribe((priority) => priorities.push(priority));
  });

  it('muestra la página actual, el total de páginas y el total de incidentes', () => {
    render([incident], 0, 3, 41);

    expect(element.querySelector('.pager')?.textContent).toContain('Página 1 de 3');
    expect(element.querySelector('.count')?.textContent?.trim()).toBe('41');
    expect(button('Página anterior').disabled).toBeTrue();
    expect(button('Página siguiente').disabled).toBeFalse();
  });

  it('pide la página siguiente y la anterior', () => {
    render([incident], 1, 3, 41);

    button('Página siguiente').click();
    button('Página anterior').click();

    expect(pages).toEqual([2, 0]);
  });

  it('no permite avanzar desde la última página', () => {
    render([incident], 2, 3, 41);

    expect(button('Página siguiente').disabled).toBeTrue();
  });

  it('sin resultados muestra el aviso y oculta el paginador', () => {
    render([], 0, 0, 0);

    expect(element.querySelector('.empty-state')?.textContent).toContain('No hay incidentes');
    expect(element.querySelector('.pager')).toBeNull();
  });

  it('delega el filtro de prioridad en la página en lugar de filtrar en el navegador', () => {
    render([incident], 0, 1, 1);
    const select = element.querySelector<HTMLSelectElement>('select[aria-label="Filtrar por prioridad"]')!;

    select.value = 'Alta';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(priorities).toEqual(['Alta']);
    expect(element.querySelectorAll('tbody tr').length).toBe(1);
  });

  it('avisa de que la búsqueda solo revisa la página actual', () => {
    render([incident], 0, 3, 41);
    const search = element.querySelector<HTMLInputElement>('.search input')!;

    search.value = 'no existe';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(element.querySelector('.page-note')?.textContent).toContain('solo revisan esta página');
  });
});
