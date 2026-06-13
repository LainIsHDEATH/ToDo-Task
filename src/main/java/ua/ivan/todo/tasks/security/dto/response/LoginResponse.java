package ua.ivan.todo.tasks.security.dto.response;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds) {
}