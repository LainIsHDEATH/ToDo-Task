package ua.ivan.todo.tasks.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.validation.SortValidator;
import ua.ivan.todo.tasks.user.dto.request.UserProfileUpdateRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.service.UserService;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "firstName",
        "lastName",
        "email");

    private final UserService userService;

    @GetMapping
    public PageResponse<UserShortResponse> findAll(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return userService.findAllShort(pageable);
    }

    @GetMapping("/{id}")
    public UserShortResponse findById(@PathVariable Long id) {
        return userService.findShortById(id);
    }

    @GetMapping("/me")
    public UserResponse findCurrentUser(Authentication authentication) {
        return userService.findCurrentUser(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponse updateCurrentUser(
        Authentication authentication,
        @Valid @RequestBody UserProfileUpdateRequest request) {
        return userService.updateCurrentUser(authentication.getName(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUser(Authentication authentication) {
        userService.deleteCurrentUser(authentication.getName());
    }
}