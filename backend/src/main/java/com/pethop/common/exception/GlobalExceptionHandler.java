package com.pethop.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException exception, HttpServletRequest request) {
        return problem(exception.status(), exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "La solicitud contiene campos no válidos.", request);
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(), (first, ignored) -> first));
        detail.setProperty("fields", fields);
        return detail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleCredentials(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.", request);
    }

    @ExceptionHandler(DisabledException.class)
    ProblemDetail handleDisabled(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "La cuenta no está habilitada.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConflict(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "La operación entra en conflicto con un registro existente.", request);
    }

    private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
