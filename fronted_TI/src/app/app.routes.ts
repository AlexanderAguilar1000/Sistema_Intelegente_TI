import { Routes } from '@angular/router';
import { IncidenteComponent } from './pages/Incidente/incidente.component';
import { IncidentDetailPageComponent } from './pages/Incidente/pages/incident-detail/incident-detail-page.component';
import { TecnicosComponent } from './pages/Tecnicos/tecnicos.component';

export const routes: Routes = [
    { path: 'incidentes', component: IncidenteComponent },
    { path: 'incidentes/:id', component: IncidentDetailPageComponent },
    { path: 'administracion', component: TecnicosComponent },
    { path: '', pathMatch: 'full', redirectTo: 'incidentes' },
];
