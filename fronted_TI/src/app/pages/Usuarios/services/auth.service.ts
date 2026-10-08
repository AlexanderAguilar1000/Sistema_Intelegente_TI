import { Injectable, signal } from '@angular/core';
import { AppUser } from '../models/user.model';

const STORAGE_KEY = 'activeUser';

/** Usuario activo de la sesión (simula el inicio de sesión). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly activeUser = signal<AppUser | null>(this.restore());

  switchUser(user: AppUser): void {
    this.activeUser.set(user);
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    } catch {
      // sin almacenamiento: el usuario activo solo vive en memoria
    }
  }

  private restore(): AppUser | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AppUser) : null;
    } catch {
      return null;
    }
  }
}
