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
import ua.ivan.todo.tasks.user.api.interfaces.UserReadFacade;
import ua.ivan.todo.tasks.user.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTaskService {

    private static final String USER_NOT_FOUND_MESSAGE = "User with id '%d' was not found";
    private static final String TASK_NOT_FOUND_MESSAGE = "Task with id '%d' was not found";
    private static final String USERS_NOT_FOUND_MESSAGE = "Users with ids %s were not found";
    private static final String OWNER_AS_COLLABORATOR_MESSAGE = "Task owner cannot be added as collaborator";

    private final TaskRepository taskRepository;
    private final UserReadFacade userReadFacade;
    private final TaskMapper taskMapper;
    private final DomainModelValidator validator;

    @Transactional(readOnly = true)
    public PageResponse<TaskListItemResponse> findAll(Pageable pageable) {
        log.info("Admin fetching all tasks. page={}, size={}, sort={}",
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return PageResponse.from(
            taskRepository.findAll(pageable)
                .map(taskMapper::toListItemResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskListItemResponse> findAllByOwnerId(Long userId, Pageable pageable) {
        log.info("Admin fetching tasks by owner. ownerId={}, page={}, size={}, sort={}",
            userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        ensureUserExists(userId);

        return PageResponse.from(
            taskRepository.findAllByOwnerId(userId, pageable)
                .map(taskMapper::toListItemResponse));
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long taskId) {
        log.info("Admin fetching task. taskId={}", taskId);

        Task task = getTaskOrThrow(taskId);

        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse create(Long ownerId, TaskCreateRequest request) {
        log.info("Admin creating task. ownerId={}, collaboratorCount={}",
            ownerId, countCollaborators(request.collaboratorIds()));

        User owner = getUserOrThrow(ownerId);

        Task task = taskMapper.toEntity(request);
        task.setOwner(owner);
        task.setStatus(TaskStatus.TODO);
        task.setCollaborators(resolveCollaborators(ownerId, request.collaboratorIds()));

        Task savedTask = taskRepository.save(validator.validate(task));

        log.info("Admin created task successfully. taskId={}, ownerId={}",
            savedTask.getId(), ownerId);

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse update(Long taskId, TaskUpdateRequest request) {
        log.info("Admin updating task. taskId={}, collaboratorCount={}",
            taskId, countCollaborators(request.collaboratorIds()));

        Task task = getTaskOrThrow(taskId);
        Long ownerId = task.getOwner().getId();

        task.setName(request.name());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        task.setCollaborators(resolveCollaborators(ownerId, request.collaboratorIds()));

        Task savedTask = taskRepository.save(validator.validate(task));

        log.info("Admin updated task successfully. taskId={}, ownerId={}",
            savedTask.getId(), ownerId);

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public void deleteById(Long taskId) {
        log.info("Admin deleting task. taskId={}", taskId);

        if (!taskRepository.existsById(taskId)) {
            throw new NotFoundException(TASK_NOT_FOUND_MESSAGE.formatted(taskId));
        }

        taskRepository.deleteById(taskId);

        log.info("Admin deleted task successfully. taskId={}", taskId);
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND_MESSAGE.formatted(taskId)));
    }

    private User getUserOrThrow(Long userId) {
        return userReadFacade.findById(userId)
            .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId)));
    }

    private void ensureUserExists(Long userId) {
        if (!userReadFacade.existsById(userId)) {
            throw new NotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId));
        }
    }

    private Set<User> resolveCollaborators(Long ownerId, Set<Long> collaboratorIds) {
        if (collaboratorIds == null || collaboratorIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> normalizedCollaboratorIds = new HashSet<>(collaboratorIds);

        if (normalizedCollaboratorIds.contains(ownerId)) {
            throw new ConflictException(OWNER_AS_COLLABORATOR_MESSAGE);
        }

        List<User> collaborators = userReadFacade.findAllByIds(normalizedCollaboratorIds);

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