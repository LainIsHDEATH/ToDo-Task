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
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Current user task endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "priority",
            "status");

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get current user's tasks")
    @ApiResponse(responseCode = "200", description = "Tasks returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public PageResponse<TaskListItemResponse> findCurrentUserTasks(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return taskService.findCurrentUserTasks(authentication.getName(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create current user's task")
    @ApiResponse(responseCode = "201", description = "Task created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "Collaborator was not found")
    @ApiResponse(responseCode = "409", description = "Owner cannot be collaborator")
    public TaskResponse create(
            Authentication authentication,
            @Valid @RequestBody TaskCreateRequest request) {
        return taskService.create(authentication.getName(), request);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get current user's task by id")
    @ApiResponse(responseCode = "200", description = "Task returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "Task was not found")
    public TaskResponse findById(
            @PathVariable Long taskId,
            Authentication authentication) {
        return taskService.findById(taskId, authentication.getName());
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update current user's task")
    @ApiResponse(responseCode = "200", description = "Task updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "Task or collaborator was not found")
    @ApiResponse(responseCode = "409", description = "Owner cannot be collaborator")
    public TaskResponse update(
            @PathVariable Long taskId,
            Authentication authentication,
            @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(taskId, authentication.getName(), request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete current user's task")
    @ApiResponse(responseCode = "204", description = "Task deleted successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "Task was not found")
    public void deleteById(
            @PathVariable Long taskId,
            Authentication authentication) {
        taskService.deleteById(taskId, authentication.getName());
    }
}