package ua.ivan.todo.tasks.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.validation.DomainModelValidator;
import ua.ivan.todo.tasks.user.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.dto.request.UserUpdateRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.mapper.UserMapper;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;
import ua.ivan.todo.tasks.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "User with id '%d' was not found";
    private static final String EMAIL_ALREADY_EXISTS_MESSAGE = "User with email '%s' already exists";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private final DomainModelValidator validator;

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        validateEmailIsUnique(request.email());

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(validator.validate(user));

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = getUserOrThrow(id);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUserOrThrow(id);

        validateEmailIsUniqueForUpdate(request.email(), id);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setRole(request.role());

        User savedUser = userRepository.save(validator.validate(user));

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException(USER_NOT_FOUND_MESSAGE.formatted(id));
        }

        userRepository.deleteById(id);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE.formatted(id)));
    }

    private void validateEmailIsUnique(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_ALREADY_EXISTS_MESSAGE.formatted(email));
        }
    }

    private void validateEmailIsUniqueForUpdate(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new ConflictException(EMAIL_ALREADY_EXISTS_MESSAGE.formatted(email));
                });
    }
}