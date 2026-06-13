package ua.ivan.todo.tasks.user.api.dto.response;

import org.springframework.modulith.NamedInterface;

@NamedInterface("UserShortResponse")
public record UserShortResponse(
    Long id,
    String firstName,
    String lastName,
    String email) {
}
