package ua.ivan.todo.tasks.task.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.validation.DomainModelValidator;
import ua.ivan.todo.tasks.task.dto.request.TaskCreateRequest;
import ua.ivan.todo.tasks.task.dto.request.TaskUpdateRequest;
import ua.ivan.todo.tasks.task.dto.response.TaskListItemResponse;
import ua.ivan.todo.tasks.task.dto.response.TaskResponse;
import ua.ivan.todo.tasks.task.mapper.TaskMapper;
import ua.ivan.todo.tasks.task.model.Task;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.task.repository.TaskRepository;
import ua.ivan.todo.tasks.user.model.User;
import ua.ivan.todo.tasks.user.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class TaskService {

    private static final String USER_NOT_FOUND_MESSAGE = "User with id '%d' was not found";
    private static final String TASK_NOT_FOUND_MESSAGE = "Task with id '%d' was not found";
    private static final String COLLABORATORS_NOT_FOUND_MESSAGE = "Users with ids %s were not found";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final DomainModelValidator validator;

    @Transactional(readOnly = true)
    public List<TaskListItemResponse> findAllByOwnerId(Long userId) {
        ensureUserExists(userId);

        return taskRepository.findAllByOwnerId(userId)
                .stream()
                .map(taskMapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long taskId) {
        Task task = getTaskOrThrow(taskId);

        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse create(Long ownerId, TaskCreateRequest request) {
        User owner = getUserOrThrow(ownerId);

        Task task = taskMapper.toEntity(request);
        task.setOwner(owner);
        task.setStatus(TaskStatus.TODO);
        task.setCollaborators(resolveCollaborators(request.collaboratorIds()));

        Task savedTask = taskRepository.save(validator.validate(task));

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse update(Long taskId, TaskUpdateRequest request) {
        Task task = getTaskOrThrow(taskId);

        task.setName(request.name());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        task.setCollaborators(resolveCollaborators(request.collaboratorIds()));

        Task savedTask = taskRepository.save(validator.validate(task));

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public void deleteById(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new NotFoundException(TASK_NOT_FOUND_MESSAGE.formatted(taskId));
        }

        taskRepository.deleteById(taskId);
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND_MESSAGE.formatted(taskId)));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId)));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId));
        }
    }

    private Set<User> resolveCollaborators(Set<Long> collaboratorIds) {
        if (collaboratorIds == null || collaboratorIds.isEmpty()) {
            return new HashSet<>();
        }

        List<User> users = userRepository.findAllById(collaboratorIds);

        if (users.size() != collaboratorIds.size()) {
            Set<Long> foundIds = users.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIds = collaboratorIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());

            throw new NotFoundException(COLLABORATORS_NOT_FOUND_MESSAGE.formatted(missingIds));
        }

        return new HashSet<>(users);
    }
}