package ua.ivan.todo.tasks.user.dto.response;

public record UserShortResponse(
    Long id,
    String firstName,
    String lastName,
    String email) {
}
