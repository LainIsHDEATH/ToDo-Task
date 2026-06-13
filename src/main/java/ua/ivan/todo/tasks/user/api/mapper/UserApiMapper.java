package ua.ivan.todo.tasks.user.api.mapper;

import org.mapstruct.Mapper;
import org.springframework.modulith.NamedInterface;
import ua.ivan.todo.tasks.common.config.MapStructConfig;
import ua.ivan.todo.tasks.user.api.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.model.User;

@Mapper(config = MapStructConfig.class)
@NamedInterface("UserApiMapper")
public interface UserApiMapper {

    UserShortResponse toShortResponse(User user);
}
