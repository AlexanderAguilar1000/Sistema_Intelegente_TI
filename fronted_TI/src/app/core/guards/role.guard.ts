import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from '../../pages/Usuarios/models/user.model';
import { AuthService } from '../../pages/Usuarios/services/auth.service';

/** Solo deja pasar al rol indicado; el resto vuelve al listado de incidentes. */
export const roleGuard =
  (role: UserRole): CanActivateFn =>
  () => {
    const user = inject(AuthService).activeUser();
    return user?.role === role ? true : inject(Router).createUrlTree(['/incidentes']);
  };
