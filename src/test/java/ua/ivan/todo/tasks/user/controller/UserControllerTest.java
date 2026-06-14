package ua.ivan.todo.tasks.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.exception.handler.GlobalExceptionHandler;
import ua.ivan.todo.tasks.user.dto.request.UserProfileUpdateRequest;
import ua.ivan.todo.tasks.user.api.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.api.dto.response.UserShortResponse;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.service.UserService;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(userController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setValidator(validator())
            .build();
    }

    @Test
    void findAllShouldReturnPagedShortUsers() throws Exception {
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

        PageResponse<UserShortResponse> response = PageResponse.from(
            new PageImpl<>(List.of(firstResponse, secondResponse)));

        when(userService.findAllShort(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].firstName").value("Nick"))
            .andExpect(jsonPath("$.content[0].lastName").value("Green"))
            .andExpect(jsonPath("$.content[0].email").value("nick@mail.com"))
            .andExpect(jsonPath("$.content[0].role").doesNotExist())
            .andExpect(jsonPath("$.content[1].id").value(2))
            .andExpect(jsonPath("$.content[1].firstName").value("Nora"))
            .andExpect(jsonPath("$.content[1].lastName").value("White"))
            .andExpect(jsonPath("$.content[1].email").value("nora@mail.com"))
            .andExpect(jsonPath("$.content[1].role").doesNotExist())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        verify(userService).findAllShort(any(Pageable.class));
    }

    @Test
    void findAllShouldPassPageableToService() throws Exception {
        when(userService.findAllShort(any(Pageable.class)))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "lastName,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(userService).findAllShort(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("lastName")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("lastName").isAscending()).isTrue();
    }

    @Test
    void findByIdShouldReturnShortUser() throws Exception {
        UserShortResponse response = new UserShortResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com");

        when(userService.findShortById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("Nick"))
            .andExpect(jsonPath("$.lastName").value("Green"))
            .andExpect(jsonPath("$.email").value("nick@mail.com"))
            .andExpect(jsonPath("$.role").doesNotExist());

        verify(userService).findShortById(1L);
    }

    @Test
    void findByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.findShortById(99L))
                .thenThrow(new NotFoundException("User with id '99' was not found"));

        mockMvc.perform(get("/api/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/users/99"));

        verify(userService).findShortById(99L);
    }

    @Test
    void findCurrentUserShouldReturnCurrentUser() throws Exception {
        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userService.findCurrentUser("nick@mail.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
            .principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("Nick"))
            .andExpect(jsonPath("$.lastName").value("Green"))
            .andExpect(jsonPath("$.email").value("nick@mail.com"))
            .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).findCurrentUser("nick@mail.com");
    }

    @Test
    void updateCurrentUserShouldUpdateCurrentUser() throws Exception {
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

        when(userService.updateCurrentUser("nick@mail.com", request)).thenReturn(response);

        mockMvc.perform(put("/api/users/me")
            .principal(authentication())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.email").value("new@mail.com"))
            .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).updateCurrentUser("nick@mail.com", request);
    }

    @Test
    void updateCurrentUserShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
            {
              "firstName": "",
              "lastName": "",
              "email": "invalid-email"
            }
            """;

        mockMvc.perform(put("/api/users/me")
            .principal(authentication())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/users/me"))
            .andExpect(jsonPath("$.fieldErrors").isArray());

        verify(userService, never()).updateCurrentUser(any(), any());
    }

    @Test
    void deleteCurrentUserShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/me")
            .principal(authentication()))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(userService).deleteCurrentUser("nick@mail.com");
    }

    @Test
    void deleteCurrentUserShouldReturnNotFoundWhenCurrentUserDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Current user was not found"))
            .when(userService)
            .deleteCurrentUser("nick@mail.com");

        mockMvc.perform(delete("/api/users/me")
            .principal(authentication()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Current user was not found"))
            .andExpect(jsonPath("$.path").value("/api/users/me"));

        verify(userService).deleteCurrentUser("nick@mail.com");
    }

    private static Principal authentication() {
        return new TestingAuthenticationToken(
            "nick@mail.com",
            null,
            "ROLE_USER");
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return validator;
    }
}