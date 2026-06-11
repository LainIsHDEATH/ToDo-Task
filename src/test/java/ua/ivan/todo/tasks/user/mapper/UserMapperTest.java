package ua.ivan.todo.tasks.user.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.ivan.todo.tasks.user.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UserMapperImpl.class)
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void toResponseShouldMapEntityToUserResponseWithoutPasswordHash() {
        User user = User.builder()
            .id(1L)
            .firstName("Nick")
            .lastName("Green")
            .email("nick@mail.com")
            .passwordHash("hashed-password")
            .role(Role.USER)
            .build();

        UserResponse response = userMapper.toResponse(user);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.firstName()).isEqualTo("Nick");
        assertThat(response.lastName()).isEqualTo("Green");
        assertThat(response.email()).isEqualTo("nick@mail.com");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    void toShortResponseShouldMapEntityToShortResponse() {
        User user = User.builder()
            .id(2L)
            .firstName("Nora")
            .lastName("White")
            .email("nora@mail.com")
            .passwordHash("hashed-password")
            .role(Role.ADMIN)
            .build();

        UserShortResponse response = userMapper.toShortResponse(user);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.firstName()).isEqualTo("Nora");
        assertThat(response.lastName()).isEqualTo("White");
        assertThat(response.email()).isEqualTo("nora@mail.com");
    }

    @Test
    void toEntityShouldMapRegistrationRequestWithoutPasswordHashAndRole() {
        UserRegistrationRequest request = new UserRegistrationRequest(
            "Mike",
            "Brown",
            "mike@mail.com",
            "password123");

        User user = userMapper.toEntity(request);

        assertThat(user.getId()).isNull();
        assertThat(user.getFirstName()).isEqualTo("Mike");
        assertThat(user.getLastName()).isEqualTo("Brown");
        assertThat(user.getEmail()).isEqualTo("mike@mail.com");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getRole()).isNull();
    }
}