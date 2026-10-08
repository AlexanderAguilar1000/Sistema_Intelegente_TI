package com.corporativoTI.SistemaInteligenteTI.shared.error;

import com.corporativoTI.SistemaInteligenteTI.users.exception.InvalidAreaException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingActiveUserException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingFieldException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.RoleNotAllowedException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.UnknownActiveUserException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de dominio a la respuesta de error homogénea de la API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingActiveUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingActiveUser(MissingActiveUserException ex) {
        return new ErrorResponse("MISSING_ACTIVE_USER", ex.getMessage());
    }

    @ExceptionHandler(UnknownActiveUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnknownActiveUser(UnknownActiveUserException ex) {
        return new ErrorResponse("UNKNOWN_ACTIVE_USER", ex.getMessage());
    }

    @ExceptionHandler(RoleNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleRoleNotAllowed(RoleNotAllowedException ex) {
        return new ErrorResponse("FORBIDDEN_ROLE", ex.getMessage());
    }

    @ExceptionHandler(MissingFieldException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleMissingField(MissingFieldException ex) {
        return new ErrorResponse("MISSING_FIELD", ex.getMessage());
    }

    @ExceptionHandler(InvalidAreaException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInvalidArea(InvalidAreaException ex) {
        return new ErrorResponse("INVALID_AREA", ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return new ErrorResponse("USERNAME_ALREADY_EXISTS", ex.getMessage());
    }
}
