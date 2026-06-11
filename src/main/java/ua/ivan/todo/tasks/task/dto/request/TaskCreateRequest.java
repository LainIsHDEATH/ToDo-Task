package ua.ivan.todo.tasks.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.ivan.todo.tasks.task.model.TaskPriority;

import java.util.Set;

public record TaskCreateRequest(

    @NotBlank(message = "Task name is required") @Size(max = 255,
        message = "Task name must not exceed 255 characters") String name,

    @NotNull(message = "Task priority is required") TaskPriority priority,

    Set<@NotNull(message = "Collaborator id must not be null") @Positive(
        message = "Collaborator id must be positive") Long> collaboratorIds) {
}