package ua.ivan.todo.tasks.task.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.validation.SortValidator;
import ua.ivan.todo.tasks.task.dto.request.TaskCreateRequest;
import ua.ivan.todo.tasks.task.dto.request.TaskUpdateRequest;
import ua.ivan.todo.tasks.task.dto.response.TaskListItemResponse;
import ua.ivan.todo.tasks.task.dto.response.TaskResponse;
import ua.ivan.todo.tasks.task.service.TaskService;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "priority",
            "status"
    );

    private final TaskService taskService;

    @GetMapping("/api/users/{userId}/tasks")
    public PageResponse<TaskListItemResponse> findAllByOwnerId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return taskService.findAllByOwnerId(userId, pageable);
    }

    @PostMapping("/api/users/{userId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @PathVariable Long userId,
            @Valid @RequestBody TaskCreateRequest request
    ) {
        return taskService.create(userId, request);
    }

    @GetMapping("/api/tasks/{taskId}")
    public TaskResponse findById(@PathVariable Long taskId) {
        return taskService.findById(taskId);
    }

    @PutMapping("/api/tasks/{taskId}")
    public TaskResponse update(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request
    ) {
        return taskService.update(taskId, request);
    }

    @DeleteMapping("/api/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long taskId) {
        taskService.deleteById(taskId);
    }
}