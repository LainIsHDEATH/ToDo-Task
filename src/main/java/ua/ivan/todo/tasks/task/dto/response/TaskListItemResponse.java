package ua.ivan.todo.tasks.task.dto.response;

import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;

public record TaskListItemResponse(
        Long id,
        String name,
        TaskPriority priority,
        TaskStatus status
) {
}
