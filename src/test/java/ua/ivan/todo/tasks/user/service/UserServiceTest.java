package ua.ivan.todo.tasks.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DomainModelValidator validator;

    @InjectMocks
    private UserService userService;

    @Test
    void registerShouldCreateUserWithEncodedPasswordAndDefaultRole() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Nick",
                "Green",
                "nick@mail.com",
                "password123"
        );

        User user = User.builder()
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .passwordHash("encoded-password")
                .role(Role.USER)
                .build();

        UserResponse response = new UserResponse(
                1L,
                "Nick",
                "Green",
                "nick@mail.com",
                Role.USER
        );

        when(userRepository.existsByEmail("nick@mail.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(validator.validate(user)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse actual = userService.register(request);

        assertThat(actual).isEqualTo(response);

        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getRole()).isEqualTo(Role.USER);

        verify(userRepository).save(user);
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void registerShouldThrowConflictExceptionWhenEmailAlreadyExists() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Nick",
                "Green",
                "nick@mail.com",
                "password123"
        );

        when(userRepository.existsByEmail("nick@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User with email 'nick@mail.com' already exists");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void findAllShouldReturnAllUsers() {
        User firstUser = User.builder()
                .id(1L)
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        User secondUser = User.builder()
                .id(2L)
                .firstName("Nora")
                .lastName("White")
                .email("nora@mail.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .build();

        UserResponse firstResponse = new UserResponse(1L, "Nick", "Green", "nick@mail.com", Role.USER);
        UserResponse secondResponse = new UserResponse(2L, "Nora", "White", "nora@mail.com", Role.ADMIN);

        when(userRepository.findAll()).thenReturn(List.of(firstUser, secondUser));
        when(userMapper.toResponse(firstUser)).thenReturn(firstResponse);
        when(userMapper.toResponse(secondUser)).thenReturn(secondResponse);

        List<UserResponse> actual = userService.findAll();

        assertThat(actual).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void findByIdShouldReturnUserWhenUserExists() {
        User user = User.builder()
                .id(1L)
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        UserResponse response = new UserResponse(1L, "Nick", "Green", "nick@mail.com", Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = userService.findById(1L);

        assertThat(actual).isEqualTo(response);
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");
    }

    @Test
    void updateShouldUpdateExistingUser() {
        User user = User.builder()
                .id(1L)
                .firstName("Old")
                .lastName("Name")
                .email("old@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        UserUpdateRequest request = new UserUpdateRequest(
                "New",
                "User",
                "new@mail.com",
                Role.ADMIN
        );

        UserResponse response = new UserResponse(
                1L,
                "New",
                "User",
                "new@mail.com",
                Role.ADMIN
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(validator.validate(user)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = userService.update(1L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getEmail()).isEqualTo("new@mail.com");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);

        verify(userRepository).save(user);
    }

    @Test
    void updateShouldAllowKeepingSameEmail() {
        User user = User.builder()
                .id(1L)
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        UserUpdateRequest request = new UserUpdateRequest(
                "Nick",
                "Green",
                "nick@mail.com",
                Role.USER
        );

        UserResponse response = new UserResponse(
                1L,
                "Nick",
                "Green",
                "nick@mail.com",
                Role.USER
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nick@mail.com")).thenReturn(Optional.of(user));
        when(validator.validate(user)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = userService.update(1L, request);

        assertThat(actual).isEqualTo(response);
    }

    @Test
    void updateShouldThrowConflictExceptionWhenEmailBelongsToAnotherUser() {
        User currentUser = User.builder()
                .id(1L)
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        User anotherUser = User.builder()
                .id(2L)
                .firstName("Nora")
                .lastName("White")
                .email("nora@mail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        UserUpdateRequest request = new UserUpdateRequest(
                "Nick",
                "Green",
                "nora@mail.com",
                Role.USER
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("nora@mail.com")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User with email 'nora@mail.com' already exists");
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UserUpdateRequest request = new UserUpdateRequest(
                "Nick",
                "Green",
                "nick@mail.com",
                Role.USER
        );

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");
    }

    @Test
    void deleteByIdShouldDeleteUserWhenUserExistsAndDoesNotOwnTasks() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteByIdShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");

        verify(userRepository, never()).deleteById(any());
    }
}