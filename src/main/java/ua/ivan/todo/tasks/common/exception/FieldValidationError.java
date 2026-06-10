package ua.ivan.todo.tasks.common.exception;

public record FieldValidationError(
        String field,
        String message,
        Object rejectedValue
) {
}