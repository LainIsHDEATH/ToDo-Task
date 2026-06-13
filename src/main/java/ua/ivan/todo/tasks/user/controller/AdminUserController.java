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
import org.springframework.web.bind.annotation.*;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.validation.SortValidator;
import ua.ivan.todo.tasks.user.dto.request.UserUpdateRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.service.AdminUserService;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "firstName",
            "lastName",
            "email",
            "role");

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Admin get users")
    @ApiResponse(responseCode = "200", description = "Users returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    public PageResponse<UserResponse> findAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return adminUserService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Admin get user by id")
    @ApiResponse(responseCode = "200", description = "User returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "User was not found")
    public UserResponse findById(@PathVariable Long id) {
        return adminUserService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin update user")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "User was not found")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return adminUserService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Admin delete user")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Admin role is required")
    @ApiResponse(responseCode = "404", description = "User was not found")
    public void deleteById(@PathVariable Long id) {
        adminUserService.deleteById(id);
    }
}