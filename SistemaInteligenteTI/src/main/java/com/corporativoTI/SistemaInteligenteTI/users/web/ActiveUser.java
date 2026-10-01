package com.corporativoTI.SistemaInteligenteTI.users.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca el parámetro de un controlador que debe recibir el usuario activo resuelto a partir
 * de la cabecera {@code X-User-Id} (RF-1).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActiveUser {}
