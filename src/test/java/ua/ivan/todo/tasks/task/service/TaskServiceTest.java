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
import ua.ivan.todo.tasks.user.api.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.api.interfaces.UserReadFacade;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserReadFacade userReadFacade;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private DomainModelValidator validator;

    @InjectMocks
    private TaskService taskService;

    @Test
    void findCurrentUserTasksShouldReturnOnlyCurrentUserTasks() {
        Pageable pageable = PageRequest.of(0, 20);

        User currentUser = user(1L, "owner@mail.com", Role.USER);

        Task firstTask = task(
            1L,
            "First task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            currentUser);

        Task secondTask = task(
            2L,
            "Second task",
            TaskPriority.MEDIUM,
            TaskStatus.IN_PROGRESS,
            currentUser);

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

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findAllByOwnerId(1L, pageable)).thenReturn(tasksPage);
        when(taskMapper.toListItemResponse(firstTask)).thenReturn(firstResponse);
        when(taskMapper.toListItemResponse(secondTask)).thenReturn(secondResponse);

        PageResponse<TaskListItemResponse> actual =
            taskService.findCurrentUserTasks("owner@mail.com", pageable);

        assertThat(actual.content()).containsExactly(firstResponse, secondResponse);
        assertThat(actual.page()).isZero();
        assertThat(actual.size()).isEqualTo(20);
        assertThat(actual.totalElements()).isEqualTo(2);
        assertThat(actual.totalPages()).isEqualTo(1);
        assertThat(actual.first()).isTrue();
        assertThat(actual.last()).isTrue();

        verify(userReadFacade).findByEmail("owner@mail.com");
        verify(taskRepository).findAllByOwnerId(1L, pageable);
    }

    @Test
    void findCurrentUserTasksShouldThrowNotFoundExceptionWhenCurrentUserDoesNotExist() {
        Pageable pageable = PageRequest.of(0, 20);

        when(userReadFacade.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findCurrentUserTasks("missing@mail.com", pageable))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Current user was not found");

        verify(userReadFacade).findByEmail("missing@mail.com");
        verifyNoInteractions(taskRepository);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void findByIdShouldReturnTaskWhenCurrentUserIsOwner() {
        User owner = user(1L, "owner@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            owner);

        TaskResponse response = taskResponse(task);

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = taskService.findById(1L, "owner@mail.com");

        assertThat(actual).isEqualTo(response);

        verify(userReadFacade).findByEmail("owner@mail.com");
        verify(taskRepository).findById(1L);
        verify(taskMapper).toResponse(task);
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenTaskDoesNotExist() {
        User owner = user(1L, "owner@mail.com", Role.USER);

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L, "owner@mail.com"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Task with id '99' was not found");

        verify(userReadFacade).findByEmail("owner@mail.com");
        verify(taskRepository).findById(99L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenTaskBelongsToAnotherUser() {
        User currentUser = user(1L, "current@mail.com", Role.USER);
        User anotherUser = user(2L, "another@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            anotherUser);

        when(userReadFacade.findByEmail("current@mail.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.findById(1L, "current@mail.com"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Task with id '1' was not found");

        verify(userReadFacade).findByEmail("current@mail.com");
        verify(taskRepository).findById(1L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void createShouldCreateTaskForCurrentUserWithoutCollaborators() {
        User owner = user(1L, "owner@mail.com", Role.USER);

        TaskCreateRequest request = new TaskCreateRequest(
            "New task",
            TaskPriority.HIGH,
            null);

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

        TaskResponse response = taskResponse(savedTask);

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(response);

        TaskResponse actual = taskService.create("owner@mail.com", request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getOwner()).isEqualTo(owner);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCollaborators()).isEmpty();

        verify(userReadFacade).findByEmail("owner@mail.com");
        verify(taskMapper).toEntity(request);
        verify(validator).validate(task);
        verify(taskRepository).save(task);
        verify(taskMapper).toResponse(savedTask);
    }

    @Test
    void createShouldCreateTaskForCurrentUserWithCollaborators() {
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

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(userReadFacade.findAllByIds(Set.of(2L))).thenReturn(List.of(collaborator));
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(response);

        TaskResponse actual = taskService.create("owner@mail.com", request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getOwner()).isEqualTo(owner);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCollaborators()).containsExactly(collaborator);

        verify(userReadFacade).findAllByIds(Set.of(2L));
        verify(taskRepository).save(task);
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

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);

        assertThatThrownBy(() -> taskService.create("owner@mail.com", request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Task owner cannot be added as collaborator");

        verify(userReadFacade).findByEmail("owner@mail.com");
        verify(taskMapper).toEntity(request);
        verify(userReadFacade, never()).findAllByIds(any());
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(validator);
    }

    @Test
    void createShouldThrowNotFoundExceptionWhenSomeCollaboratorsDoNotExist() {
        User owner = user(1L, "owner@mail.com", Role.USER);
        User collaborator = user(2L, "collaborator@mail.com", Role.USER);

        TaskCreateRequest request = new TaskCreateRequest(
            "New task",
            TaskPriority.HIGH,
            Set.of(2L, 3L));

        Task task = Task.builder()
            .name("New task")
            .priority(TaskPriority.HIGH)
            .build();

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(userReadFacade.findAllByIds(Set.of(2L, 3L))).thenReturn(List.of(collaborator));

        assertThatThrownBy(() -> taskService.create("owner@mail.com", request))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Users with ids [3] were not found");

        verify(userReadFacade).findAllByIds(Set.of(2L, 3L));
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(validator);
    }

    @Test
    void updateShouldUpdateOwnedTask() {
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
            TaskStatus.IN_PROGRESS,
            Set.of(2L));

        TaskResponse response = taskResponse(task);

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userReadFacade.findAllByIds(Set.of(2L))).thenReturn(List.of(collaborator));
        when(validator.validate(task)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse actual = taskService.update(1L, "owner@mail.com", request);

        assertThat(actual).isEqualTo(response);
        assertThat(task.getName()).isEqualTo("Updated task");
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getCollaborators()).containsExactly(collaborator);

        verify(taskRepository).save(task);
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenTaskBelongsToAnotherUser() {
        User currentUser = user(1L, "current@mail.com", Role.USER);
        User anotherUser = user(2L, "another@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.LOW,
            TaskStatus.TODO,
            anotherUser);

        TaskUpdateRequest request = new TaskUpdateRequest(
            "Updated task",
            TaskPriority.HIGH,
            TaskStatus.IN_PROGRESS,
            Set.of());

        when(userReadFacade.findByEmail("current@mail.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(1L, "current@mail.com", request))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Task with id '1' was not found");

        verify(taskRepository, never()).save(any());
        verifyNoInteractions(validator);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void deleteByIdShouldDeleteOwnedTask() {
        User owner = user(1L, "owner@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            owner);

        when(userReadFacade.findByEmail("owner@mail.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.deleteById(1L, "owner@mail.com");

        verify(userReadFacade).findByEmail("owner@mail.com");
        verify(taskRepository).findById(1L);
        verify(taskRepository).delete(task);
    }

    @Test
    void deleteByIdShouldThrowNotFoundExceptionWhenTaskBelongsToAnotherUser() {
        User currentUser = user(1L, "current@mail.com", Role.USER);
        User anotherUser = user(2L, "another@mail.com", Role.USER);

        Task task = task(
            1L,
            "Task",
            TaskPriority.HIGH,
            TaskStatus.TODO,
            anotherUser);

        when(userReadFacade.findByEmail("current@mail.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.deleteById(1L, "current@mail.com"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Task with id '1' was not found");

        verify(taskRepository, never()).delete(any());
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