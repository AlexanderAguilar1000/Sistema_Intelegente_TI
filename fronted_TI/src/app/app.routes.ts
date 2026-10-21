import { Routes } from '@angular/router';
import { roleGuard } from './core/guards/role.guard';
import { IncidenteComponent } from './pages/Incidente/incidente.component';
import { IncidentDetailPageComponent } from './pages/Incidente/pages/incident-detail/incident-detail-page.component';
import { TecnicosComponent } from './pages/Tecnicos/tecnicos.component';

export const routes: Routes = [
    { path: 'incidentes', component: IncidenteComponent, canActivate: [roleGuard('Supervisor', '/mis-asignados')] },
    { path: 'incidentes/:id', component: IncidentDetailPageComponent },
    { path: 'mis-asignados', component: IncidenteComponent, canActivate: [roleGuard('Técnico', '/incidentes')], data: { view: 'technician' } },
    { path: 'administracion', component: TecnicosComponent },
    { path: '', pathMatch: 'full', redirectTo: 'incidentes' },
];
