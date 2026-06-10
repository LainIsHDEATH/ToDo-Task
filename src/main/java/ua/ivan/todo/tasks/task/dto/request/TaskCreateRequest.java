package ua.ivan.todo.tasks.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.ivan.todo.tasks.task.model.TaskPriority;

import java.util.Set;

public record TaskCreateRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        TaskPriority priority,

        Set<Long> collaboratorIds
) {
}