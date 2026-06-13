package ua.ivan.todo.tasks.user.controller;

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
public class AdminUserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "firstName",
        "lastName",
        "email",
        "role");

    private final AdminUserService adminUserService;

    @GetMapping
    public PageResponse<UserResponse> findAll(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        SortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);

        return adminUserService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return adminUserService.findById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request) {
        return adminUserService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        adminUserService.deleteById(id);
    }
}