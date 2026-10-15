import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../pages/Usuarios/services/auth.service';

/** Añade la cabecera X-User-Id con el usuario activo a las llamadas a /api (el backend la exige en @ActiveUser). */
//pasa esto X-User-Id ala cabecera del http 
export const activeUserInterceptor: HttpInterceptorFn = (req, next) => {
  const user = inject(AuthService).activeUser();
  if (!user || !req.url.startsWith('/api')) return next(req);
  return next(req.clone({ setHeaders: { 'X-User-Id': String(user.id) } }));
};
