package ua.ivan.todo.tasks.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ua.ivan.todo.tasks.common.exception.exceptions.AuthenticationException;
import ua.ivan.todo.tasks.common.exception.handler.GlobalExceptionHandler;
import ua.ivan.todo.tasks.security.dto.request.LoginRequest;
import ua.ivan.todo.tasks.security.dto.response.LoginResponse;
import ua.ivan.todo.tasks.security.service.AuthService;
import ua.ivan.todo.tasks.user.dto.request.UserRegistrationRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.service.UserService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator())
            .build();
    }

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest(
            "Nick",
            "Green",
            "nick@mail.com",
            "password123");

        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userService.register(request)).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("Nick"))
            .andExpect(jsonPath("$.lastName").value("Green"))
            .andExpect(jsonPath("$.email").value("nick@mail.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).register(request);
    }

    @Test
    void registerShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
            {
              "firstName": "",
              "lastName": "",
              "email": "invalid-email",
              "password": "short"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/auth/register"))
            .andExpect(jsonPath("$.fieldErrors").isArray());

        verifyNoInteractions(userService);
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest(
            "nick@mail.com",
            "password123");

        LoginResponse response = new LoginResponse(
            "jwt-token",
            "Bearer",
            3600L);

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresInSeconds").value(3600));

        verify(authService).login(request);
    }

    @Test
    void loginShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
            {
              "email": "invalid-email",
              "password": ""
            }
            """;

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/auth/login"))
            .andExpect(jsonPath("$.fieldErrors").isArray());

        verifyNoInteractions(authService);
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest(
            "nick@mail.com",
            "wrong-password");

        when(authService.login(request))
            .thenThrow(new AuthenticationException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
            .andExpect(jsonPath("$.path").value("/api/auth/login"));

        verify(authService).login(request);
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}