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
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.validation.DomainModelValidator;
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
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DomainModelValidator validator;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void findAllShouldReturnPagedUsers() {
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

        UserResponse firstResponse = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        UserResponse secondResponse = new UserResponse(
            2L,
            "Nora",
            "White",
            "nora@mail.com",
            Role.ADMIN);

        Page<User> usersPage = new PageImpl<>(
            List.of(firstUser, secondUser),
            pageable,
            2);

        when(userRepository.findAll(pageable)).thenReturn(usersPage);
        when(userMapper.toResponse(firstUser)).thenReturn(firstResponse);
        when(userMapper.toResponse(secondUser)).thenReturn(secondResponse);

        PageResponse<UserResponse> actual = adminUserService.findAll(pageable);

        assertThat(actual.content()).containsExactly(firstResponse, secondResponse);
        assertThat(actual.page()).isZero();
        assertThat(actual.size()).isEqualTo(20);
        assertThat(actual.totalElements()).isEqualTo(2);
        assertThat(actual.totalPages()).isEqualTo(1);
        assertThat(actual.first()).isTrue();
        assertThat(actual.last()).isTrue();

        verify(userRepository).findAll(pageable);
        verify(userMapper).toResponse(firstUser);
        verify(userMapper).toResponse(secondUser);
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

        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = adminUserService.findById(1L);

        assertThat(actual).isEqualTo(response);

        verify(userRepository).findById(1L);
        verify(userMapper).toResponse(user);
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");

        verify(userRepository).findById(99L);
        verifyNoInteractions(userMapper);
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
            Role.ADMIN);

        UserResponse response = new UserResponse(
            1L,
            "New",
            "User",
            "new@mail.com",
            Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(validator.validate(user)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = adminUserService.update(1L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getEmail()).isEqualTo("new@mail.com");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);

        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("new@mail.com");
        verify(validator).validate(user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
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
            Role.USER);

        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nick@mail.com")).thenReturn(Optional.of(user));
        when(validator.validate(user)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual = adminUserService.update(1L, request);

        assertThat(actual).isEqualTo(response);
        assertThat(user.getEmail()).isEqualTo("nick@mail.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);

        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("nick@mail.com");
        verify(userRepository).save(user);
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
            Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("nora@mail.com")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> adminUserService.update(1L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("User with email 'nora@mail.com' already exists");

        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("nora@mail.com");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(validator);
        verifyNoInteractions(userMapper);
    }

    @Test
    void updateShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UserUpdateRequest request = new UserUpdateRequest(
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.update(99L, request))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("User with id '99' was not found");

        verify(userRepository).findById(99L);
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(validator);
        verifyNoInteractions(userMapper);
    }

    @Test
    void deleteByIdShouldDeleteUserWhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        adminUserService.deleteById(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteByIdShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.deleteById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id '99' was not found");

        verify(userRepository).existsById(99L);
        verify(userRepository, never()).deleteById(any());
    }
}