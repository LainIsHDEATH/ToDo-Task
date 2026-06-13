package ua.ivan.todo.tasks.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.validation.DomainModelValidator;
import ua.ivan.todo.tasks.user.dto.request.UserProfileUpdateRequest;
import ua.ivan.todo.tasks.user.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.mapper.UserMapper;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;
import ua.ivan.todo.tasks.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "User with id '%d' was not found";
    private static final String CURRENT_USER_NOT_FOUND_MESSAGE = "Current user was not found";
    private static final String EMAIL_ALREADY_EXISTS_MESSAGE = "User with email '%s' already exists";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DomainModelValidator validator;

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        log.info("Registering user. email={}", request.email());

        validateEmailIsUnique(request.email());

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(validator.validate(user));

        log.info("User registered successfully. userId={}, email={}",
                savedUser.getId(), savedUser.getEmail());

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserShortResponse> findAllShort(Pageable pageable) {
        log.info("Fetching user catalog. page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return PageResponse.from(
                userRepository.findAll(pageable)
                        .map(userMapper::toShortResponse));
    }

    @Transactional(readOnly = true)
    public UserShortResponse findShortById(Long id) {
        log.info("Fetching public user information. userId={}", id);

        User user = getUserOrThrow(id);

        return userMapper.toShortResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findCurrentUser(String currentUserEmail) {
        log.info("Fetching current user profile. email={}", currentUserEmail);

        User user = getUserByEmailOrThrow(currentUserEmail);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(String currentUserEmail, UserProfileUpdateRequest request) {
        log.info("Updating current user profile. currentEmail={}, newEmail={}",
                currentUserEmail, request.email());

        User user = getUserByEmailOrThrow(currentUserEmail);

        validateEmailIsUniqueForUpdate(request.email(), user.getId());

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());

        User savedUser = userRepository.save(validator.validate(user));

        log.info("Current user profile updated successfully. userId={}, email={}",
                savedUser.getId(), savedUser.getEmail());

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public void deleteCurrentUser(String currentUserEmail) {
        log.info("Deleting current user profile. email={}", currentUserEmail);

        User user = getUserByEmailOrThrow(currentUserEmail);

        userRepository.delete(user);

        log.info("Current user profile deleted successfully. userId={}, email={}",
                user.getId(), user.getEmail());
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE.formatted(id)));
    }

    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(CURRENT_USER_NOT_FOUND_MESSAGE));
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