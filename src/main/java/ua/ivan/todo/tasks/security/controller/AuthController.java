package ua.ivan.todo.tasks.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ua.ivan.todo.tasks.user.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegistrationRequest request) {
        return userService.register(request);
    }
}