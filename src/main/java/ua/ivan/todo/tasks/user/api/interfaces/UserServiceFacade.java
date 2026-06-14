package ua.ivan.todo.tasks.user.api.interfaces;

import org.springframework.modulith.NamedInterface;
import ua.ivan.todo.tasks.user.api.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.api.dto.response.UserResponse;

@NamedInterface("UserServiceFacade")
public interface UserServiceFacade {

    UserResponse register(UserRegistrationRequest request);
}