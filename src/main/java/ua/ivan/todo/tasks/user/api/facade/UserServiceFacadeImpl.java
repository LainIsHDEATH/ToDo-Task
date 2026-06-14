package ua.ivan.todo.tasks.user.api.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.ivan.todo.tasks.user.api.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.api.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.api.interfaces.UserServiceFacade;
import ua.ivan.todo.tasks.user.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceFacadeImpl implements UserServiceFacade {
    private final UserService userService;

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {

        return userService.register(request);
    }
}
