package ua.ivan.todo.tasks.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.exception.handler.GlobalExceptionHandler;
import ua.ivan.todo.tasks.task.dto.request.TaskCreateRequest;
import ua.ivan.todo.tasks.task.dto.request.TaskUpdateRequest;
import ua.ivan.todo.tasks.task.dto.response.TaskListItemResponse;
import ua.ivan.todo.tasks.task.dto.response.TaskResponse;
import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.task.service.TaskService;
import ua.ivan.todo.tasks.user.dto.response.UserShortResponse;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator())
                .build();
    }

    @Test
    void findAllByOwnerIdShouldReturnUserTasks() throws Exception {
        List<TaskListItemResponse> tasks = List.of(
                new TaskListItemResponse(1L, "First task", TaskPriority.HIGH, TaskStatus.TODO),
                new TaskListItemResponse(2L, "Second task", TaskPriority.LOW, TaskStatus.DONE)
        );

        when(taskService.findAllByOwnerId(1L)).thenReturn(tasks);

        mockMvc.perform(get("/api/users/{userId}/tasks", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("First task"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Second task"))
                .andExpect(jsonPath("$[1].priority").value("LOW"))
                .andExpect(jsonPath("$[1].status").value("DONE"));
    }

    @Test
    void findAllByOwnerIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(taskService.findAllByOwnerId(99L))
                .thenThrow(new NotFoundException("User with id '99' was not found"));

        mockMvc.perform(get("/api/users/{userId}/tasks", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/users/99/tasks"));
    }

    @Test
    void findByIdShouldReturnTask() throws Exception {
        TaskResponse response = new TaskResponse(
                10L,
                "Task",
                TaskPriority.MEDIUM,
                TaskStatus.TODO,
                new UserShortResponse(1L, "Nick", "Green", "nick@mail.com"),
                Set.of(new UserShortResponse(2L, "Nora", "White", "nora@mail.com"))
        );

        when(taskService.findById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/tasks/{taskId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Task"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.owner.id").value(1))
                .andExpect(jsonPath("$.collaborators").isArray());
    }

    @Test
    void findByIdShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        when(taskService.findById(99L))
                .thenThrow(new NotFoundException("Task with id '99' was not found"));

        mockMvc.perform(get("/api/tasks/{taskId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/99"));
    }

    @Test
    void createShouldReturnCreatedTask() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
                "New task",
                TaskPriority.HIGH,
                Set.of(2L)
        );

        TaskResponse response = new TaskResponse(
                10L,
                "New task",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                new UserShortResponse(1L, "Nick", "Green", "nick@mail.com"),
                Set.of(new UserShortResponse(2L, "Nora", "White", "nora@mail.com"))
        );

        when(taskService.create(1L, request)).thenReturn(response);

        mockMvc.perform(post("/api/users/{userId}/tasks", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("New task"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.owner.id").value(1))
                .andExpect(jsonPath("$.collaborators").isArray());
    }

    @Test
    void createShouldNormalizeDuplicateCollaboratorIdsInRequest() throws Exception {
        String body = """
                {
                  "name": "New task",
                  "priority": "HIGH",
                  "collaboratorIds": [2, 2, 2]
                }
                """;

        TaskResponse response = new TaskResponse(
                10L,
                "New task",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                new UserShortResponse(1L, "Nick", "Green", "nick@mail.com"),
                Set.of(new UserShortResponse(2L, "Nora", "White", "nora@mail.com"))
        );

        when(taskService.create(eq(1L), any(TaskCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/{userId}/tasks", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("New task"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.owner.id").value(1))
                .andExpect(jsonPath("$.collaborators").isArray());

        ArgumentCaptor<TaskCreateRequest> requestCaptor = ArgumentCaptor.forClass(TaskCreateRequest.class);

        verify(taskService).create(eq(1L), requestCaptor.capture());

        assertThat(requestCaptor.getValue().collaboratorIds())
                .containsExactly(2L);
    }

    @Test
    void createShouldReturnConflictWhenOwnerIsCollaborator() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
                "New task",
                TaskPriority.HIGH,
                Set.of(1L)
        );

        when(taskService.create(1L, request))
                .thenThrow(new ConflictException("Task owner cannot be added as collaborator"));

        mockMvc.perform(post("/api/users/{userId}/tasks", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Task owner cannot be added as collaborator"))
                .andExpect(jsonPath("$.path").value("/api/users/1/tasks"));
    }

    @Test
    void createShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
                {
                  "name": "",
                  "priority": null
                }
                """;

        mockMvc.perform(post("/api/users/{userId}/tasks", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/users/1/tasks"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updateShouldReturnUpdatedTask() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.LOW,
                TaskStatus.DONE,
                Set.of(2L)
        );

        TaskResponse response = new TaskResponse(
                10L,
                "Updated task",
                TaskPriority.LOW,
                TaskStatus.DONE,
                new UserShortResponse(1L, "Nick", "Green", "nick@mail.com"),
                Set.of(new UserShortResponse(2L, "Nora", "White", "nora@mail.com"))
        );

        when(taskService.update(10L, request)).thenReturn(response);

        mockMvc.perform(put("/api/tasks/{taskId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Updated task"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.owner.id").value(1))
                .andExpect(jsonPath("$.collaborators").isArray());
    }

    @Test
    void updateShouldNormalizeDuplicateCollaboratorIdsInRequest() throws Exception {
        String body = """
                {
                  "name": "Updated task",
                  "priority": "HIGH",
                  "status": "IN_PROGRESS",
                  "collaboratorIds": [2, 2, 2]
                }
                """;

        TaskResponse response = new TaskResponse(
                10L,
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                new UserShortResponse(1L, "Nick", "Green", "nick@mail.com"),
                Set.of(new UserShortResponse(2L, "Nora", "White", "nora@mail.com"))
        );

        when(taskService.update(eq(10L), any(TaskUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/tasks/{taskId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Updated task"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.owner.id").value(1))
                .andExpect(jsonPath("$.collaborators").isArray());

        ArgumentCaptor<TaskUpdateRequest> requestCaptor = ArgumentCaptor.forClass(TaskUpdateRequest.class);

        verify(taskService).update(eq(10L), requestCaptor.capture());

        assertThat(requestCaptor.getValue().collaboratorIds())
                .containsExactly(2L);
    }

    @Test
    void updateShouldReturnConflictWhenOwnerIsCollaborator() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                Set.of(1L)
        );

        when(taskService.update(10L, request))
                .thenThrow(new ConflictException("Task owner cannot be added as collaborator"));

        mockMvc.perform(put("/api/tasks/{taskId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Task owner cannot be added as collaborator"))
                .andExpect(jsonPath("$.path").value("/api/tasks/10"));
    }

    @Test
    void updateShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
                {
                  "name": "",
                  "priority": null,
                  "status": null
                }
                """;

        mockMvc.perform(put("/api/tasks/{taskId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/tasks/10"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updateShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                Set.of()
        );

        when(taskService.update(99L, request))
                .thenThrow(new NotFoundException("Task with id '99' was not found"));

        mockMvc.perform(put("/api/tasks/{taskId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/99"));
    }

    @Test
    void deleteByIdShouldReturnNoContent() throws Exception {
        doNothing().when(taskService).deleteById(10L);

        mockMvc.perform(delete("/api/tasks/{taskId}", 10L))
                .andExpect(status().isNoContent());

        verify(taskService).deleteById(10L);
    }

    @Test
    void deleteByIdShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Task with id '99' was not found"))
                .when(taskService).deleteById(99L);

        mockMvc.perform(delete("/api/tasks/{taskId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/99"));
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return validator;
    }
}