package ua.ivan.todo.tasks.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.validation.DomainModelValidator;
import ua.ivan.todo.tasks.user.mapper.UserMapper;
import ua.ivan.todo.tasks.user.dto.request.UserProfileUpdateRequest;
import ua.ivan.todo.tasks.user.api.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.api.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.api.dto.response.UserShortResponse;
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
            "password123");

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
            Role.USER);

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

        verify(userRepository).existsByEmail("nick@mail.com");
        verify(userMapper).toEntity(request);
        verify(passwordEncoder).encode("password123");
        verify(validator).validate(user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(savedUser);
    }

    @Test
    void registerShouldThrowConflictExceptionWhenEmailAlreadyExists() {
        UserRegistrationRequest request = new UserRegistrationRequest(
            "Nick",
            "Green",
            "nick@mail.com",
            "password123");

        when(userRepository.existsByEmail("nick@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("User with email 'nick@mail.com' already exists");

        verify(userRepository).existsByEmail("nick@mail.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verifyNoInteractions(userMapper);
        verifyNoInteractions(validator);
    }

    @Test
    void findAllShortShouldReturnPagedUsers() {
        Pageable pageable = PageRequest.of(0, 20);

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

        UserShortResponse firstResponse = new UserShortResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com");

        UserShortResponse secondResponse = new UserShortResponse(
            2L,
            "Nora",
            "White",
            "nora@mail.com");

        Page<User> usersPage = new PageImpl<>(
            List.of(firstUser, secondUser),
            pageable,
            2);

        when(userRepository.findAll(pageable)).thenReturn(usersPage);
        when(userMapper.toShortResponse(firstUser)).thenReturn(firstResponse);
        when(userMapper.toShortResponse(secondUser)).thenReturn(secondResponse);

        PageResponse<UserShortResponse> actual = userService.findAllShort(pageable);

        assertThat(actual.content()).containsExactly(firstResponse, secondResponse);
        assertThat(actual.page()).isZero();
        assertThat(actual.size()).isEqualTo(20);
        assertThat(actual.totalElements()).isEqualTo(2);
        assertThat(actual.totalPages()).isEqualTo(1);
        assertThat(actual.first()).isTrue();
        assertThat(actual.last()).isTrue();

        verify(userRepository).findAll(pageable);
        verify(userMapper).toShortResponse(firstUser);
        verify(userMapper).toShortResponse(secondUser);
    }

    @Test
    void findShortByIdShouldReturnUserWhenUserExists() {
        User user = User.builder()
            .id(1L)
            .firstName("Nick")
            .lastName("Green")
            .email("nick@mail.com")
            .passwordHash("hash")
            .role(Role.USER)
            .build();

        UserShortResponse response = new UserShortResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toShortResponse(user)).thenReturn(response);

        UserShortResponse actual = userService.findShortById(1L);

        assertThat(actual).isEqualTo(response);

        verify(userRepository).findById(1L);
        verify(userMapper).toShortResponse(user);
    }

    @Test
    void findShortByIdShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findShortById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");

        verify(userRepository).findById(99L);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findCurrentUserShouldReturnCurrentUser() {
        User user = User.builder()
            .id(1L)
            .firstName("Nick")
            .lastName("Green")
            .email("nick@mail.com")
            .passwordHash("hash")
            .role(Role.USER)
            .build();

        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userRepository.findByEmail("nick@mail.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = userService.findCurrentUser("nick@mail.com");

        assertThat(actual).isEqualTo(response);

        verify(userRepository).findByEmail("nick@mail.com");
        verify(userMapper).toResponse(user);
    }

    @Test
    void findCurrentUserShouldThrowNotFoundExceptionWhenCurrentUserDoesNotExist() {
        when(userRepository.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findCurrentUser("missing@mail.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Current user was not found");

        verify(userRepository).findByEmail("missing@mail.com");
        verifyNoInteractions(userMapper);
    }

    @Test
    void updateCurrentUserShouldUpdateCurrentUserProfile() {
        User user = User.builder()
            .id(1L)
            .firstName("Old")
            .lastName("Name")
            .email("old@mail.com")
            .passwordHash("hash")
            .role(Role.USER)
            .build();

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
            "New",
            "User",
            "new@mail.com");

        UserResponse response = new UserResponse(
            1L,
            "New",
            "User",
            "new@mail.com",
            Role.USER);

        when(userRepository.findByEmail("old@mail.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(validator.validate(user)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = userService.updateCurrentUser("old@mail.com", request);

        assertThat(actual).isEqualTo(response);
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getEmail()).isEqualTo("new@mail.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);

        verify(userRepository).findByEmail("old@mail.com");
        verify(userRepository).findByEmail("new@mail.com");
        verify(validator).validate(user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateCurrentUserShouldAllowKeepingSameEmail() {
        User user = User.builder()
            .id(1L)
            .firstName("Nick")
            .lastName("Green")
            .email("nick@mail.com")
            .passwordHash("hash")
            .role(Role.USER)
            .build();

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
            "Nick",
            "Green",
            "nick@mail.com");

        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userRepository.findByEmail("nick@mail.com")).thenReturn(Optional.of(user));
        when(validator.validate(user)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = userService.updateCurrentUser("nick@mail.com", request);

        assertThat(actual).isEqualTo(response);
        assertThat(user.getEmail()).isEqualTo("nick@mail.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);

        verify(userRepository, times(2)).findByEmail("nick@mail.com");
        verify(userRepository).save(user);
    }

    @Test
    void updateCurrentUserShouldThrowConflictExceptionWhenEmailBelongsToAnotherUser() {
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

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
            "Nick",
            "Green",
            "nora@mail.com");

        when(userRepository.findByEmail("nick@mail.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("nora@mail.com")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateCurrentUser("nick@mail.com", request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("User with email 'nora@mail.com' already exists");

        verify(userRepository).findByEmail("nick@mail.com");
        verify(userRepository).findByEmail("nora@mail.com");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(validator);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void updateCurrentUserShouldThrowNotFoundExceptionWhenCurrentUserDoesNotExist() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
            "Nick",
            "Green",
            "nick@mail.com");

        when(userRepository.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateCurrentUser("missing@mail.com", request))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Current user was not found");

        verify(userRepository).findByEmail("missing@mail.com");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(validator);
        verifyNoInteractions(userMapper);
    }

    @Test
    void deleteCurrentUserShouldDeleteCurrentUser() {
        User user = User.builder()
            .id(1L)
            .firstName("Nick")
            .lastName("Green")
            .email("nick@mail.com")
            .passwordHash("hash")
            .role(Role.USER)
            .build();

        when(userRepository.findByEmail("nick@mail.com")).thenReturn(Optional.of(user));

        userService.deleteCurrentUser("nick@mail.com");

        verify(userRepository).findByEmail("nick@mail.com");
        verify(userRepository).delete(user);
    }

    @Test
    void deleteCurrentUserShouldThrowNotFoundExceptionWhenCurrentUserDoesNotExist() {
        when(userRepository.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteCurrentUser("missing@mail.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Current user was not found");

        verify(userRepository).findByEmail("missing@mail.com");
        verify(userRepository, never()).delete(any());
    }
}