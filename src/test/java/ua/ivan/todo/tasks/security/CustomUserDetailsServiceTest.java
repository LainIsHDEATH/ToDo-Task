package ua.ivan.todo.tasks.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ua.ivan.todo.tasks.security.service.CustomUserDetailsService;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;
import ua.ivan.todo.tasks.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
        User user = User.builder()
                .id(1L)
                .firstName("Nick")
                .lastName("Green")
                .email("nick@mail.com")
                .passwordHash("$2a$10$encoded-password")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("nick@mail.com"))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("nick@mail.com");

        assertThat(userDetails.getUsername()).isEqualTo("nick@mail.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encoded-password");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsernameShouldConvertAdminRoleToGrantedAuthority() {
        User user = User.builder()
                .id(2L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@mail.com")
                .passwordHash("$2a$10$encoded-password")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmail("admin@mail.com"))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@mail.com");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsernameShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@mail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@mail.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User with email 'missing@mail.com' was not found");
    }
}