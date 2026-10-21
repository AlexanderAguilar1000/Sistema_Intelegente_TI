import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from '../../pages/Usuarios/models/user.model';
import { AuthService } from '../../pages/Usuarios/services/auth.service';

/**
 * Solo deja pasar al rol indicado; el resto va a `fallback`.
 * Sin usuario activo deja pasar: el selector de usuario se abre solo y la vista reacciona al elegir.
 */
export const roleGuard =
  (role: UserRole, fallback: string): CanActivateFn =>
  () => {
    const user = inject(AuthService).activeUser();
    return !user || user.role === role ? true : inject(Router).createUrlTree([fallback]);
  };
