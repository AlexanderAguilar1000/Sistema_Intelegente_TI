package com.corporativoTI.SistemaInteligenteTI.users.exception;

/** Ya existe un usuario con ese nombre de usuario (RF-2). */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Ya existe un usuario con el nombre de usuario '" + username + "'.");
    }
}
