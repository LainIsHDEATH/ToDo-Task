package ua.ivan.todo.tasks.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.exception.handler.GlobalExceptionHandler;
import ua.ivan.todo.tasks.task.dto.request.TaskCreateRequest;
import ua.ivan.todo.tasks.task.dto.request.TaskUpdateRequest;
import ua.ivan.todo.tasks.task.dto.response.TaskListItemResponse;
import ua.ivan.todo.tasks.task.dto.response.TaskResponse;
import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.task.service.AdminTaskService;
import ua.ivan.todo.tasks.user.api.dto.response.UserShortResponse;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminTaskControllerTest {

    @Mock
    private AdminTaskService adminTaskService;

    @InjectMocks
    private AdminTaskController adminTaskController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(adminTaskController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setValidator(validator())
            .build();
    }

    @Test
    void findAllShouldReturnPagedTasks() throws Exception {
        TaskListItemResponse firstResponse = new TaskListItemResponse(
            1L,
            "First task",
            TaskPriority.HIGH,
            TaskStatus.TODO);

        TaskListItemResponse secondResponse = new TaskListItemResponse(
            2L,
            "Second task",
            TaskPriority.MEDIUM,
            TaskStatus.IN_PROGRESS);

        PageResponse<TaskListItemResponse> response = PageResponse.from(
            new PageImpl<>(List.of(firstResponse, secondResponse)));

        when(adminTaskService.findAll(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/admin/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].name").value("First task"))
            .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
            .andExpect(jsonPath("$.content[0].status").value("TODO"))
            .andExpect(jsonPath("$.content[1].id").value(2))
            .andExpect(jsonPath("$.content[1].name").value("Second task"))
            .andExpect(jsonPath("$.content[1].priority").value("MEDIUM"))
            .andExpect(jsonPath("$.content[1].status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        verify(adminTaskService).findAll(any(Pageable.class));
    }

    @Test
    void findAllShouldPassPageableToService() throws Exception {
        when(adminTaskService.findAll(any(Pageable.class)))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/admin/tasks")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "status,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(adminTaskService).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("status")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("status").isDescending()).isTrue();
    }

    @Test
    void findAllByOwnerIdShouldReturnPagedTasks() throws Exception {
        TaskListItemResponse responseItem = new TaskListItemResponse(
            1L,
            "Owner task",
            TaskPriority.HIGH,
            TaskStatus.TODO);

        PageResponse<TaskListItemResponse> response = PageResponse.from(
            new PageImpl<>(List.of(responseItem)));

        when(adminTaskService.findAllByOwnerId(eq(10L), any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(get("/api/admin/users/{userId}/tasks", 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Owner task"))
            .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
            .andExpect(jsonPath("$.content[0].status").value("TODO"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(adminTaskService).findAllByOwnerId(eq(10L), any(Pageable.class));
    }

    @Test
    void findAllByOwnerIdShouldReturnNotFoundWhenOwnerDoesNotExist() throws Exception {
        when(adminTaskService.findAllByOwnerId(eq(99L), any(Pageable.class)))
                .thenThrow(new NotFoundException("User with id '99' was not found"));

        mockMvc.perform(get("/api/admin/users/{userId}/tasks", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/admin/users/99/tasks"));

        verify(adminTaskService).findAllByOwnerId(eq(99L), any(Pageable.class));
    }

    @Test
    void createShouldReturnCreatedTask() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
            "New task",
            TaskPriority.HIGH,
            Set.of(2L));

        TaskResponse response = taskResponse(
            1L,
            "New task",
            TaskPriority.HIGH,
            TaskStatus.TODO);

        when(adminTaskService.create(10L, request)).thenReturn(response);

        mockMvc.perform(post("/api/admin/users/{userId}/tasks", 10L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("New task"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.owner.id").value(10))
            .andExpect(jsonPath("$.owner.email").value("owner@mail.com"));

        verify(adminTaskService).create(10L, request);
    }

    @Test
    void createShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
            {
              "name": "",
              "priority": null,
              "collaboratorIds": [-1]
            }
            """;

        mockMvc.perform(post("/api/admin/users/{userId}/tasks", 10L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/admin/users/10/tasks"))
            .andExpect(jsonPath("$.fieldErrors").isArray());

        verify(adminTaskService, never()).create(any(), any());
    }

    @Test
    void findByIdShouldReturnTask() throws Exception {
        TaskResponse response = taskResponse(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO);

        when(adminTaskService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/tasks/{taskId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Task"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.owner.email").value("owner@mail.com"));

        verify(adminTaskService).findById(1L);
    }

    @Test
    void findByIdShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        when(adminTaskService.findById(99L))
                .thenThrow(new NotFoundException("Task with id '99' was not found"));

        mockMvc.perform(get("/api/admin/tasks/{taskId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/admin/tasks/99"));

        verify(adminTaskService).findById(99L);
    }

    @Test
    void updateShouldReturnUpdatedTask() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
            "Updated task",
            TaskPriority.MEDIUM,
            TaskStatus.IN_PROGRESS,
            Set.of(2L));

        TaskResponse response = taskResponse(
            1L,
            "Updated task",
            TaskPriority.MEDIUM,
            TaskStatus.IN_PROGRESS);

        when(adminTaskService.update(1L, request)).thenReturn(response);

        mockMvc.perform(put("/api/admin/tasks/{taskId}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Updated task"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.owner.email").value("owner@mail.com"));

        verify(adminTaskService).update(1L, request);
    }

    @Test
    void updateShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
            {
              "name": "",
              "priority": null,
              "status": null,
              "collaboratorIds": [-1]
            }
            """;

        mockMvc.perform(put("/api/admin/tasks/{taskId}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/admin/tasks/1"))
            .andExpect(jsonPath("$.fieldErrors").isArray());

        verify(adminTaskService, never()).update(any(), any());
    }

    @Test
    void deleteByIdShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/tasks/{taskId}", 1L))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(adminTaskService).deleteById(1L);
    }

    @Test
    void deleteByIdShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Task with id '99' was not found"))
            .when(adminTaskService)
            .deleteById(99L);

        mockMvc.perform(delete("/api/admin/tasks/{taskId}", 99L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Task with id '99' was not found"))
            .andExpect(jsonPath("$.path").value("/api/admin/tasks/99"));

        verify(adminTaskService).deleteById(99L);
    }

    private static TaskResponse taskResponse(
        Long id,
        String name,
        TaskPriority priority,
        TaskStatus status) {
        UserShortResponse owner = new UserShortResponse(
            10L,
            "Task",
            "Owner",
            "owner@mail.com");

        return new TaskResponse(
            id,
            name,
            priority,
            status,
            owner,
            Set.of());
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return validator;
    }
}