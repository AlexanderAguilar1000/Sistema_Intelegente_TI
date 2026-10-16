import { Routes } from '@angular/router';
import { IncidenteComponent } from './pages/Incidente/incidente.component';
import { TecnicosComponent } from './pages/Tecnicos/tecnicos.component';

export const routes: Routes = [
    { path: 'incidentes', component: IncidenteComponent },
    { path: 'administracion', component: TecnicosComponent },
    { path: '', pathMatch: 'full', redirectTo: 'incidentes' },
];
