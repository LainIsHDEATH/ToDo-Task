package ua.ivan.todo.tasks.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
@Slf4j
public class TaskService {

    private static final String TASK_NOT_FOUND_MESSAGE = "Task with id '%d' was not found";
    private static final String CURRENT_USER_NOT_FOUND_MESSAGE = "Current user was not found";
    private static final String USERS_NOT_FOUND_MESSAGE = "Users with ids %s were not found";
    private static final String OWNER_AS_COLLABORATOR_MESSAGE = "Task owner cannot be added as collaborator";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final DomainModelValidator validator;

    @Transactional(readOnly = true)
    public PageResponse<TaskListItemResponse> findCurrentUserTasks(
            String currentUserEmail,
            Pageable pageable) {
        log.info("Fetching current user tasks. email={}, page={}, size={}, sort={}",
                currentUserEmail, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        User currentUser = getCurrentUserOrThrow(currentUserEmail);

        return PageResponse.from(
                taskRepository.findAllByOwnerId(currentUser.getId(), pageable)
                        .map(taskMapper::toListItemResponse));
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long taskId, String currentUserEmail) {
        log.info("Fetching current user task. taskId={}, email={}", taskId, currentUserEmail);

        User currentUser = getCurrentUserOrThrow(currentUserEmail);
        Task task = getOwnedTaskOrThrow(taskId, currentUser);

        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse create(String currentUserEmail, TaskCreateRequest request) {
        log.info("Creating current user task. email={}, collaboratorCount={}",
                currentUserEmail, countCollaborators(request.collaboratorIds()));

        User currentUser = getCurrentUserOrThrow(currentUserEmail);

        Task task = taskMapper.toEntity(request);
        task.setOwner(currentUser);
        task.setStatus(TaskStatus.TODO);
        task.setCollaborators(resolveCollaborators(currentUser.getId(), request.collaboratorIds()));

        Task savedTask = taskRepository.save(validator.validate(task));

        log.info("Current user task created successfully. taskId={}, ownerId={}",
                savedTask.getId(), currentUser.getId());

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse update(Long taskId, String currentUserEmail, TaskUpdateRequest request) {
        log.info("Updating current user task. taskId={}, email={}, collaboratorCount={}",
                taskId, currentUserEmail, countCollaborators(request.collaboratorIds()));

        User currentUser = getCurrentUserOrThrow(currentUserEmail);
        Task task = getOwnedTaskOrThrow(taskId, currentUser);

        task.setName(request.name());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        task.setCollaborators(resolveCollaborators(currentUser.getId(), request.collaboratorIds()));

        Task savedTask = taskRepository.save(validator.validate(task));

        log.info("Current user task updated successfully. taskId={}, ownerId={}",
                savedTask.getId(), currentUser.getId());

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public void deleteById(Long taskId, String currentUserEmail) {
        log.info("Deleting current user task. taskId={}, email={}", taskId, currentUserEmail);

        User currentUser = getCurrentUserOrThrow(currentUserEmail);
        Task task = getOwnedTaskOrThrow(taskId, currentUser);

        taskRepository.delete(task);

        log.info("Current user task deleted successfully. taskId={}, ownerId={}",
                taskId, currentUser.getId());
    }

    private Task getOwnedTaskOrThrow(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND_MESSAGE.formatted(taskId)));

        if (!task.getOwner().getId().equals(currentUser.getId())) {
            throw new NotFoundException(TASK_NOT_FOUND_MESSAGE.formatted(taskId));
        }

        return task;
    }

    private User getCurrentUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(CURRENT_USER_NOT_FOUND_MESSAGE));
    }

    private Set<User> resolveCollaborators(Long ownerId, Set<Long> collaboratorIds) {
        if (collaboratorIds == null || collaboratorIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> normalizedCollaboratorIds = new HashSet<>(collaboratorIds);

        if (normalizedCollaboratorIds.contains(ownerId)) {
            throw new ConflictException(OWNER_AS_COLLABORATOR_MESSAGE);
        }

        List<User> collaborators = userRepository.findAllById(normalizedCollaboratorIds);

        if (collaborators.size() != normalizedCollaboratorIds.size()) {
            Set<Long> foundIds = collaborators.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIds = normalizedCollaboratorIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toCollection(HashSet::new));

            throw new NotFoundException(USERS_NOT_FOUND_MESSAGE.formatted(missingIds));
        }

        return new HashSet<>(collaborators);
    }

    private int countCollaborators(Set<Long> collaboratorIds) {
        if (collaboratorIds == null) {
            return 0;
        }

        return collaboratorIds.size();
    }
}