package com.corporativoTI.SistemaInteligenteTI.shared.error;

import com.corporativoTI.SistemaInteligenteTI.users.service.MissingActiveUserException;
import com.corporativoTI.SistemaInteligenteTI.users.service.UnknownActiveUserException;
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
}
