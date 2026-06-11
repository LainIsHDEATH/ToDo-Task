package ua.ivan.todo.tasks.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.exception.handler.GlobalExceptionHandler;
import ua.ivan.todo.tasks.user.dto.request.UserUpdateRequest;
import ua.ivan.todo.tasks.user.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    void findAllShouldReturnDefaultPage() throws Exception {
        PageResponse<UserResponse> response = new PageResponse<>(
            List.of(new UserResponse(1L, "Nick", "Green", "nick@mail.com", Role.USER)),
            0,
            20,
            1,
            1,
            true,
            true);

        when(userService.findAll(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].email").value("nick@mail.com"))
            .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(userService).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        Sort.Order order = pageable.getSort().getOrderFor("id");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void findAllShouldUseCustomPageAndSize() throws Exception {
        PageResponse<UserResponse> response = new PageResponse<>(
            List.of(),
            2,
            5,
            0,
            0,
            false,
            true);

        when(userService.findAll(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/users")
            .param("page", "2")
            .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(5));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(userService).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    void findAllShouldUseValidSortField() throws Exception {
        PageResponse<UserResponse> response = new PageResponse<>(
            List.of(),
            0,
            20,
            0,
            0,
            true,
            true);

        when(userService.findAll(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/users")
            .param("sort", "email,desc"))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(userService).findAll(pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("email");

        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAllShouldReturnBadRequestWhenSortFieldIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users")
            .param("sort", "passwordHash,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Invalid sort field 'passwordHash'"))
            .andExpect(jsonPath("$.path").value("/api/users"));

        verify(userService, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAllShouldReturnEmptyPage() throws Exception {
        PageResponse<UserResponse> response = new PageResponse<>(
            List.of(),
            0,
            20,
            0,
            0,
            true,
            true);

        when(userService.findAll(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void findByIdShouldReturnUser() throws Exception {
        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(userService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("Nick"))
            .andExpect(jsonPath("$.lastName").value("Green"))
            .andExpect(jsonPath("$.email").value("nick@mail.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void findByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.findById(99L))
                .thenThrow(new NotFoundException("User with id '99' was not found"));

        mockMvc.perform(get("/api/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/users/99"));
    }

    @Test
    void updateShouldReturnUpdatedUser() throws Exception {
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

        when(userService.update(1L, request)).thenReturn(response);

        mockMvc.perform(put("/api/users/{id}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.email").value("new@mail.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void updateShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        String body = """
            {
              "firstName": "",
              "lastName": "",
              "email": "invalid-email",
              "role": null
            }
            """;

        mockMvc.perform(put("/api/users/{id}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/users/1"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void deleteByIdShouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteById(1L);

        mockMvc.perform(delete("/api/users/{id}", 1L))
            .andExpect(status().isNoContent());

        verify(userService).deleteById(1L);
    }

    @Test
    void deleteByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        doThrow(new NotFoundException("User with id '99' was not found"))
            .when(userService).deleteById(99L);

        mockMvc.perform(delete("/api/users/{id}", 99L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("User with id '99' was not found"))
            .andExpect(jsonPath("$.path").value("/api/users/99"));
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}