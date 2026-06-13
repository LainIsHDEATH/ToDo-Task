package ua.ivan.todo.tasks.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "User-facing user endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "firstName",
        "lastName",
        "email");

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get user catalog",
        description = "Returns public user information for collaborator selection.")
    @ApiResponse(responseCode = "200", description = "Users returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public PageResponse<UserShortResponse> findAll(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return userService.findAllShort(pageable);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @ApiResponse(responseCode = "200", description = "Current user returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "Current user was not found")
    public UserResponse findCurrentUser(Authentication authentication) {
        return userService.findCurrentUser(authentication.getName());
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    @ApiResponse(responseCode = "200", description = "Current user updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "Current user was not found")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    public UserResponse updateCurrentUser(
        Authentication authentication,
        @Valid @RequestBody UserProfileUpdateRequest request) {
        return userService.updateCurrentUser(authentication.getName(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete current user profile")
    @ApiResponse(responseCode = "204", description = "Current user deleted successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "Current user was not found")
    public void deleteCurrentUser(Authentication authentication) {
        userService.deleteCurrentUser(authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public user information by id")
    @ApiResponse(responseCode = "200", description = "User returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "404", description = "User was not found")
    public UserShortResponse findById(@PathVariable Long id) {
        return userService.findShortById(id);
    }
}