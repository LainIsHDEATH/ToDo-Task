package ua.ivan.todo.tasks.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.ivan.todo.tasks.config.MapStructConfig;
import ua.ivan.todo.tasks.user.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.model.User;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);

    UserShortResponse toShortResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(UserRegistrationRequest request);
}