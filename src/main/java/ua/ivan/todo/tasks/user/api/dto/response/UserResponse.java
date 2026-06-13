package ua.ivan.todo.tasks.user.api.dto.response;

import org.springframework.modulith.NamedInterface;
import ua.ivan.todo.tasks.user.model.Role;

@NamedInterface("UserResponse")
public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    Role role) {
}
