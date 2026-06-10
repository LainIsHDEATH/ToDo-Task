package ua.ivan.todo.tasks.common.exception.exceptions;

public class DeleteConflictException extends RuntimeException {
    public DeleteConflictException(String message) {
        super(message);
    }
}
