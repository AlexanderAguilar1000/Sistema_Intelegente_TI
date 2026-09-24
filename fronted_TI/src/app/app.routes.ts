import { Routes } from '@angular/router';
import { TecnicosComponent } from './pages/Tecnicos/tecnicos.component';

export const routes: Routes = [
    { path: 'administracion', component: TecnicosComponent },
    { path: '', pathMatch: 'full', redirectTo: 'administracion' },
];
