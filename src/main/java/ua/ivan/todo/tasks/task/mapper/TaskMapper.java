package ua.ivan.todo.tasks.task.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.ivan.todo.tasks.common.config.MapStructConfig;
import ua.ivan.todo.tasks.task.dto.request.TaskCreateRequest;
import ua.ivan.todo.tasks.task.dto.response.TaskListItemResponse;
import ua.ivan.todo.tasks.task.dto.response.TaskResponse;
import ua.ivan.todo.tasks.task.model.Task;
import ua.ivan.todo.tasks.user.api.mapper.UserApiMapper;

@Mapper(
    config = MapStructConfig.class,
    uses = {UserApiMapper.class})
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    TaskListItemResponse toListItemResponse(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "collaborators", ignore = true)
    Task toEntity(TaskCreateRequest request);
}