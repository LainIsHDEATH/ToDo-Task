package ua.ivan.todo.tasks.user.dto.response;

import ua.ivan.todo.tasks.user.model.Role;

public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    Role role) {
}
