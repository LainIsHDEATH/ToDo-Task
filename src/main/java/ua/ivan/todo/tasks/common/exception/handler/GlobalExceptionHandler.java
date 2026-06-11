package ua.ivan.todo.tasks.common.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.ivan.todo.tasks.common.exception.ErrorResponse;
import ua.ivan.todo.tasks.common.exception.FieldValidationError;
import ua.ivan.todo.tasks.common.exception.exceptions.BadRequestException;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.DeleteConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldValidationError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()))
            .toList();

        return ErrorResponse.withFieldErrors(
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            request.getRequestURI(),
            fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = exception.getConstraintViolations()
            .stream()
            .map(violation -> new FieldValidationError(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()))
            .toList();

        return ErrorResponse.withFieldErrors(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request.getRequestURI(),
            fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(
        HttpServletRequest request) {
        return ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            "Malformed JSON request",
            request.getRequestURI());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(
        BadRequestException exception,
        HttpServletRequest request) {
        return ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            exception.getMessage(),
            request.getRequestURI());
    }

    @ExceptionHandler({
        NotFoundException.class,
        UsernameNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
        NotFoundException exception,
        HttpServletRequest request) {
        return ErrorResponse.of(
            HttpStatus.NOT_FOUND,
            exception.getMessage(),
            request.getRequestURI());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(
        ConflictException exception,
        HttpServletRequest request) {
        return ErrorResponse.of(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            request.getRequestURI());
    }

    @ExceptionHandler(DeleteConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDeleteConflict(
        DeleteConflictException exception,
        HttpServletRequest request) {
        return ErrorResponse.of(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(
        HttpServletRequest request) {
        return ErrorResponse.of(
            HttpStatus.CONFLICT,
            "Data integrity violation",
            request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(
        HttpServletRequest request) {
        return ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected server error",
            request.getRequestURI());
    }
}