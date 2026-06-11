package ua.ivan.todo.tasks.task.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.validation.DomainModelValidator;
import ua.ivan.todo.tasks.task.dto.request.TaskCreateRequest;
import ua.ivan.todo.tasks.task.dto.request.TaskUpdateRequest;
import ua.ivan.todo.tasks.task.dto.response.TaskListItemResponse;
import ua.ivan.todo.tasks.task.dto.response.TaskResponse;
import ua.ivan.todo.tasks.task.mapper.TaskMapper;
import ua.ivan.todo.tasks.task.model.Task;
import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.task.repository.TaskRepository;
import ua.ivan.todo.tasks.user.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;
import ua.ivan.todo.tasks.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private DomainModelValidator validator;

    @InjectMocks
    private TaskService taskService;

    @Test
    void findAllByOwnerIdShouldReturnUserTasks() {
        Task firstTask = Task.builder()
                .id(1L)
                .name("First task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .build();

        Task secondTask = Task.builder()
                .id(2L)
                .name("Second task")
                .priority(TaskPriority.LOW)
                .status(TaskStatus.DONE)
                .build();

        TaskListItemResponse firstResponse = new TaskListItemResponse(
                1L,
                "First task",
                TaskPriority.HIGH,
                TaskStatus.TODO
        );

        TaskListItemResponse secondResponse = new TaskListItemResponse(
                2L,
                "Second task",
                TaskPriority.LOW,
                TaskStatus.DONE
        );

        when(userRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.findAllByOwnerId(1L)).thenReturn(List.of(firstTask, secondTask));
        when(taskMapper.toListItemResponse(firstTask)).thenReturn(firstResponse);
        when(taskMapper.toListItemResponse(secondTask)).thenReturn(secondResponse);

        List<TaskListItemResponse> actual = taskService.findAllByOwnerId(1L);

        assertThat(actual).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void findAllByOwnerIdShouldThrowNotFoundExceptionWhenOwnerDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.findAllByOwnerId(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");

        verify(taskRepository, never()).findAllByOwnerId(any());
    }

    @Test
    void findByIdShouldReturnTask() {
        User owner = User.builder()
                .id(1L)
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        Task task = Task.builder()
                .id(10L)
                .name("Task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .owner(owner)
                .build();

        TaskResponse response = new TaskResponse(
                10L,
                "Task",
                TaskPriority.MEDIUM,
                TaskStatus.TODO,
                new UserShortResponse(1L, "Nick", "Green", "nick@mail.com"),
                Set.of()
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = taskService.findById(10L);

        assertThat(actual).isEqualTo(response);
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task with id '99' was not found");
    }

    @Test
    void createShouldCreateTaskWithOwnerDefaultStatusAndCollaborators() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        User collaborator = User.builder()
                .id(2L)
                .firstName("Collaborator")
                .lastName("User")
                .email("collaborator@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        TaskCreateRequest request = new TaskCreateRequest(
                "New task",
                TaskPriority.HIGH,
                Set.of(2L)
        );

        Task task = Task.builder()
                .name("New task")
                .priority(TaskPriority.HIGH)
                .build();

        Task savedTask = Task.builder()
                .id(10L)
                .name("New task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .owner(owner)
                .collaborators(Set.of(collaborator))
                .build();

        TaskResponse response = new TaskResponse(
                10L,
                "New task",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                new UserShortResponse(1L, "Owner", "User", "owner@mail.com"),
                Set.of(new UserShortResponse(2L, "Collaborator", "User", "collaborator@mail.com"))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(collaborator));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(response);

        TaskResponse actual = taskService.create(1L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getOwner()).isEqualTo(owner);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCollaborators()).containsExactlyInAnyOrder(collaborator);

        verify(taskRepository).save(task);
    }

    @Test
    void createShouldThrowNotFoundExceptionWhenOwnerDoesNotExist() {
        TaskCreateRequest request = new TaskCreateRequest(
                "New task",
                TaskPriority.HIGH,
                Set.of()
        );

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(99L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createShouldThrowNotFoundExceptionWhenCollaboratorDoesNotExist() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        TaskCreateRequest request = new TaskCreateRequest(
                "New task",
                TaskPriority.HIGH,
                Set.of(2L)
        );

        Task task = Task.builder()
                .name("New task")
                .priority(TaskPriority.HIGH)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of());

        assertThatThrownBy(() -> taskService.create(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Users with ids [2] were not found");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createShouldThrowConflictExceptionWhenOwnerIsCollaborator() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        TaskCreateRequest request = new TaskCreateRequest(
                "New task",
                TaskPriority.HIGH,
                Set.of(1L)
        );

        Task task = Task.builder()
                .name("New task")
                .priority(TaskPriority.HIGH)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);

        assertThatThrownBy(() -> taskService.create(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Task owner cannot be added as collaborator");

        verify(userRepository, never()).findAllById(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateShouldUpdateTask() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        User collaborator = User.builder()
                .id(2L)
                .firstName("Collaborator")
                .lastName("User")
                .email("collaborator@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        Task task = Task.builder()
                .id(10L)
                .name("Old task")
                .priority(TaskPriority.LOW)
                .status(TaskStatus.TODO)
                .owner(owner)
                .build();

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                Set.of(2L)
        );

        TaskResponse response = new TaskResponse(
                10L,
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                new UserShortResponse(1L, "Owner", "User", "owner@mail.com"),
                Set.of(new UserShortResponse(2L, "Collaborator", "User", "collaborator@mail.com"))
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(collaborator));
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = taskService.update(10L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getName()).isEqualTo("Updated task");
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getCollaborators()).containsExactlyInAnyOrder(collaborator);

        verify(taskRepository).save(task);
    }

    @Test
    void updateShouldReplaceFullCollaboratorSet() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        User oldCollaborator = User.builder()
                .id(2L)
                .firstName("Old")
                .lastName("Collaborator")
                .email("old@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        User newCollaborator = User.builder()
                .id(3L)
                .firstName("New")
                .lastName("Collaborator")
                .email("new@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        Task task = Task.builder()
                .id(10L)
                .name("Old task")
                .priority(TaskPriority.LOW)
                .status(TaskStatus.TODO)
                .owner(owner)
                .collaborators(Set.of(oldCollaborator))
                .build();

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                Set.of(3L)
        );

        TaskResponse response = new TaskResponse(
                10L,
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                new UserShortResponse(1L, "Owner", "User", "owner@mail.com"),
                Set.of(new UserShortResponse(3L, "New", "Collaborator", "new@mail.com"))
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findAllById(Set.of(3L))).thenReturn(List.of(newCollaborator));
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = taskService.update(10L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getCollaborators())
                .containsExactlyInAnyOrder(newCollaborator)
                .doesNotContain(oldCollaborator);

        verify(taskRepository).save(task);
    }

    @Test
    void updateShouldRemoveAllCollaboratorsWhenEmptySetIsProvided() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        User collaborator = User.builder()
                .id(2L)
                .firstName("Collaborator")
                .lastName("User")
                .email("collaborator@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        Task task = Task.builder()
                .id(10L)
                .name("Task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .owner(owner)
                .collaborators(Set.of(collaborator))
                .build();

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Task",
                TaskPriority.HIGH,
                TaskStatus.DONE,
                Set.of()
        );

        TaskResponse response = new TaskResponse(
                10L,
                "Task",
                TaskPriority.HIGH,
                TaskStatus.DONE,
                new UserShortResponse(1L, "Owner", "User", "owner@mail.com"),
                Set.of()
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = taskService.update(10L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getCollaborators()).isEmpty();

        verify(userRepository, never()).findAllById(any());
        verify(taskRepository).save(task);
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenCollaboratorDoesNotExist() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        Task task = Task.builder()
                .id(10L)
                .name("Task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .owner(owner)
                .build();

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                Set.of(99L)
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findAllById(Set.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> taskService.update(10L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Users with ids [99] were not found");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateShouldThrowConflictExceptionWhenOwnerIsCollaborator() {
        User owner = User.builder()
                .id(1L)
                .firstName("Owner")
                .lastName("User")
                .email("owner@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        Task task = Task.builder()
                .id(10L)
                .name("Task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .owner(owner)
                .build();

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.MEDIUM,
                TaskStatus.IN_PROGRESS,
                Set.of(1L)
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(10L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Task owner cannot be added as collaborator");

        verify(userRepository, never()).findAllById(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenTaskDoesNotExist() {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated task",
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                Set.of()
        );

        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task with id '99' was not found");
    }

    @Test
    void deleteByIdShouldDeleteTask() {
        when(taskRepository.existsById(10L)).thenReturn(true);

        taskService.deleteById(10L);

        verify(taskRepository).deleteById(10L);
    }

    @Test
    void deleteByIdShouldThrowNotFoundExceptionWhenTaskDoesNotExist() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task with id '99' was not found");

        verify(taskRepository, never()).deleteById(any());
    }
}