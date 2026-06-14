package ua.ivan.todo.tasks.task.dto.response;

import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.user.api.dto.response.UserShortResponse;

import java.util.Set;

public record TaskResponse(
    Long id,
    String name,
    TaskPriority priority,
    TaskStatus status,
    UserShortResponse owner,
    Set<UserShortResponse> collaborators) {
}