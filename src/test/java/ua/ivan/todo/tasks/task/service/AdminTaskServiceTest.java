package ua.ivan.todo.tasks.task.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private DomainModelValidator validator;

    @InjectMocks
    private AdminTaskService adminTaskService;

    @Test
    void findAllShouldReturnPagedTasks() {
        Pageable pageable = PageRequest.of(0, 20);

        User owner = user(1L, "owner@mail.com", Role.USER);

        Task firstTask = task(
            1L,
            "First task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            owner);

        Task secondTask = task(
            2L,
            "Second task",
            TaskPriority.MEDIUM,
            TaskStatus.IN_PROGRESS,
            owner);

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

        Page<Task> tasksPage = new PageImpl<>(
            List.of(firstTask, secondTask),
            pageable,
            2);

        when(taskRepository.findAll(pageable)).thenReturn(tasksPage);
        when(taskMapper.toListItemResponse(firstTask)).thenReturn(firstResponse);
        when(taskMapper.toListItemResponse(secondTask)).thenReturn(secondResponse);

        PageResponse<TaskListItemResponse> actual = adminTaskService.findAll(pageable);

        assertThat(actual.content()).containsExactly(firstResponse, secondResponse);
        assertThat(actual.page()).isZero();
        assertThat(actual.size()).isEqualTo(20);
        assertThat(actual.totalElements()).isEqualTo(2);
        assertThat(actual.totalPages()).isEqualTo(1);
        assertThat(actual.first()).isTrue();
        assertThat(actual.last()).isTrue();

        verify(taskRepository).findAll(pageable);
    }

    @Test
    void findAllByOwnerIdShouldReturnPagedTasksWhenOwnerExists() {
        Pageable pageable = PageRequest.of(0, 20);

        User owner = user(1L, "owner@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            owner);

        TaskListItemResponse response = new TaskListItemResponse(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO);

        Page<Task> tasksPage = new PageImpl<>(
            List.of(task),
            pageable,
            1);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.findAllByOwnerId(1L, pageable)).thenReturn(tasksPage);
        when(taskMapper.toListItemResponse(task)).thenReturn(response);

        PageResponse<TaskListItemResponse> actual =
            adminTaskService.findAllByOwnerId(1L, pageable);

        assertThat(actual.content()).containsExactly(response);
        assertThat(actual.totalElements()).isEqualTo(1);

        verify(userRepository).existsById(1L);
        verify(taskRepository).findAllByOwnerId(1L, pageable);
    }

    @Test
    void findAllByOwnerIdShouldThrowNotFoundExceptionWhenOwnerDoesNotExist() {
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminTaskService.findAllByOwnerId(99L, pageable))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("User with id '99' was not found");

        verify(userRepository).existsById(99L);
        verifyNoInteractions(taskRepository);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void findByIdShouldReturnTaskWhenTaskExists() {
        User owner = user(1L, "owner@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            owner);

        TaskResponse response = taskResponse(task);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = adminTaskService.findById(1L);

        assertThat(actual).isEqualTo(response);

        verify(taskRepository).findById(1L);
        verify(taskMapper).toResponse(task);
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTaskService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task with id '99' was not found");

        verify(taskRepository).findById(99L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void createShouldCreateTaskForOwner() {
        User owner = user(1L, "owner@mail.com", Role.USER);
        User collaborator = user(2L, "collaborator@mail.com", Role.USER);

        TaskCreateRequest request = new TaskCreateRequest(
            "New task",
            TaskPriority.HIGH,
            Set.of(2L));

        Task task = Task.builder()
            .name("New task")
            .priority(TaskPriority.HIGH)
            .build();

        Task savedTask = task(
            1L,
            "New task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            owner);

        savedTask.setCollaborators(Set.of(collaborator));

        TaskResponse response = taskResponse(savedTask);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(collaborator));
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(response);

        TaskResponse actual = adminTaskService.create(1L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getOwner()).isEqualTo(owner);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCollaborators()).containsExactly(collaborator);

        verify(userRepository).findById(1L);
        verify(taskRepository).save(task);
    }

    @Test
    void createShouldThrowNotFoundExceptionWhenOwnerDoesNotExist() {
        TaskCreateRequest request = new TaskCreateRequest(
            "New task",
            TaskPriority.HIGH,
            Set.of());

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTaskService.create(99L, request))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("User with id '99' was not found");

        verify(userRepository).findById(99L);
        verifyNoInteractions(taskRepository);
        verifyNoInteractions(taskMapper);
        verifyNoInteractions(validator);
    }

    @Test
    void createShouldThrowConflictExceptionWhenOwnerIsCollaborator() {
        User owner = user(1L, "owner@mail.com", Role.USER);

        TaskCreateRequest request = new TaskCreateRequest(
            "New task",
            TaskPriority.HIGH,
            Set.of(1L));

        Task task = Task.builder()
            .name("New task")
            .priority(TaskPriority.HIGH)
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);

        assertThatThrownBy(() -> adminTaskService.create(1L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Task owner cannot be added as collaborator");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).findAllById(any());
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(validator);
    }

    @Test
    void updateShouldUpdateExistingTask() {
        User owner = user(1L, "owner@mail.com", Role.USER);
        User collaborator = user(2L, "collaborator@mail.com", Role.USER);

        Task task = task(
            1L,
            "Old task",
            TaskPriority.LOW,
            TaskStatus.TODO,
            owner);

        TaskUpdateRequest request = new TaskUpdateRequest(
            "Updated task",
            TaskPriority.HIGH,
            TaskStatus.DONE,
            Set.of(2L));

        TaskResponse response = taskResponse(task);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findAllById(Set.of(2L))).thenReturn(List.of(collaborator));
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = adminTaskService.update(1L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getName()).isEqualTo("Updated task");
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCollaborators()).containsExactly(collaborator);

        verify(taskRepository).findById(1L);
        verify(taskRepository).save(task);
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenTaskDoesNotExist() {
        TaskUpdateRequest request = new TaskUpdateRequest(
            "Updated task",
            TaskPriority.HIGH,
            TaskStatus.DONE,
            Set.of());

        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTaskService.update(99L, request))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Task with id '99' was not found");

        verify(taskRepository).findById(99L);
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(validator);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void updateShouldThrowConflictExceptionWhenOwnerIsCollaborator() {
        User owner = user(1L, "owner@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.LOW,
            TaskStatus.TODO,
            owner);

        TaskUpdateRequest request = new TaskUpdateRequest(
            "Updated task",
            TaskPriority.HIGH,
            TaskStatus.DONE,
            Set.of(1L));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> adminTaskService.update(1L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Task owner cannot be added as collaborator");

        verify(taskRepository).findById(1L);
        verify(userRepository, never()).findAllById(any());
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(validator);
    }

    @Test
    void deleteByIdShouldDeleteTaskWhenTaskExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        adminTaskService.deleteById(1L);

        verify(taskRepository).existsById(1L);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteByIdShouldThrowNotFoundExceptionWhenTaskDoesNotExist() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminTaskService.deleteById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task with id '99' was not found");

        verify(taskRepository).existsById(99L);
        verify(taskRepository, never()).deleteById(any());
    }

    private static User user(Long id, String email, Role role) {
        return User.builder()
            .id(id)
            .firstName("First" + id)
            .lastName("Last" + id)
            .email(email)
            .passwordHash("hash")
            .role(role)
            .build();
    }

    private static Task task(
        Long id,
        String name,
        TaskPriority priority,
        TaskStatus status,
        User owner) {
        return Task.builder()
            .id(id)
            .name(name)
            .priority(priority)
            .status(status)
            .owner(owner)
            .collaborators(new HashSet<>())
            .build();
    }

    private static TaskResponse taskResponse(Task task) {
        UserShortResponse owner = new UserShortResponse(
            task.getOwner().getId(),
            task.getOwner().getFirstName(),
            task.getOwner().getLastName(),
            task.getOwner().getEmail());

        return new TaskResponse(
            task.getId(),
            task.getName(),
            task.getPriority(),
            task.getStatus(),
            owner,
            Set.of());
    }
}