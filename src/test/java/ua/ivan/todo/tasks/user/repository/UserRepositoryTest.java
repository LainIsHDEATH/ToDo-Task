package ua.ivan.todo.tasks.user.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailShouldReturnUserWhenEmailExists() {
        User user = User.builder()
            .firstName("Nick")
            .lastName("Green")
            .email("nick@mail.com")
            .passwordHash("hashed-password")
            .role(Role.USER)
            .build();

        userRepository.save(user);

        Optional<User> actual = userRepository.findByEmail("nick@mail.com");

        assertThat(actual).isPresent();
        assertThat(actual.get().getEmail()).isEqualTo("nick@mail.com");
    }

    @Test
    void existsByEmailShouldReturnTrueWhenEmailExists() {
        User user = User.builder()
            .firstName("Nora")
            .lastName("White")
            .email("nora@mail.com")
            .passwordHash("hashed-password")
            .role(Role.USER)
            .build();

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("nora@mail.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailShouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("missing@mail.com");

        assertThat(exists).isFalse();
    }
}