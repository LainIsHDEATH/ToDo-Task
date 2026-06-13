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
import ua.ivan.todo.tasks.task.service.AdminTaskService;

import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTaskController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "name",
        "priority",
        "status");

    private final AdminTaskService adminTaskService;

    @GetMapping("/tasks")
    public PageResponse<TaskListItemResponse> findAll(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return adminTaskService.findAll(pageable);
    }

    @GetMapping("/users/{userId}/tasks")
    public PageResponse<TaskListItemResponse> findAllByOwnerId(
        @PathVariable Long userId,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return adminTaskService.findAllByOwnerId(userId, pageable);
    }

    @PostMapping("/users/{userId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
        @PathVariable Long userId,
        @Valid @RequestBody TaskCreateRequest request) {
        return adminTaskService.create(userId, request);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse findById(@PathVariable Long taskId) {
        return adminTaskService.findById(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(
        @PathVariable Long taskId,
        @Valid @RequestBody TaskUpdateRequest request) {
        return adminTaskService.update(taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long taskId) {
        adminTaskService.deleteById(taskId);
    }
}