package ua.ivan.todo.tasks.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Tasks", description = "Admin task management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminTaskController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "priority",
            "status");

    private final AdminTaskService adminTaskService;

    @GetMapping("/tasks")
    @Operation(summary = "Admin get all tasks")
    @ApiResponse(responseCode = "200", description = "Tasks returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    public PageResponse<TaskListItemResponse> findAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return adminTaskService.findAll(pageable);
    }

    @GetMapping("/users/{userId}/tasks")
    @Operation(summary = "Admin get tasks by owner")
    @ApiResponse(responseCode = "200", description = "Tasks returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "User was not found")
    public PageResponse<TaskListItemResponse> findAllByOwnerId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return adminTaskService.findAllByOwnerId(userId, pageable);
    }

    @PostMapping("/users/{userId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Admin create task for user")
    @ApiResponse(responseCode = "201", description = "Task created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "User or collaborator was not found")
    @ApiResponse(responseCode = "409", description = "Owner cannot be collaborator")
    public TaskResponse create(
            @PathVariable Long userId,
            @Valid @RequestBody TaskCreateRequest request) {
        return adminTaskService.create(userId, request);
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Admin get task by id")
    @ApiResponse(responseCode = "200", description = "Task returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "Task was not found")
    public TaskResponse findById(@PathVariable Long taskId) {
        return adminTaskService.findById(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "Admin update task")
    @ApiResponse(responseCode = "200", description = "Task updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "Task or collaborator was not found")
    @ApiResponse(responseCode = "409", description = "Owner cannot be collaborator")
    public TaskResponse update(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        return adminTaskService.update(taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Admin delete task")
    @ApiResponse(responseCode = "204", description = "Task deleted successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "Task was not found")
    public void deleteById(@PathVariable Long taskId) {
        adminTaskService.deleteById(taskId);
    }
}