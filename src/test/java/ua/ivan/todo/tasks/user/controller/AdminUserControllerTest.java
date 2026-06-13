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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ua.ivan.todo.tasks.common.dto.response.PageResponse;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;
import ua.ivan.todo.tasks.common.exception.handler.GlobalExceptionHandler;
import ua.ivan.todo.tasks.user.dto.request.UserUpdateRequest;
import ua.ivan.todo.tasks.user.api.dto.response.UserResponse;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.service.AdminUserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(adminUserController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setValidator(validator())
            .build();
    }

    @Test
    void findAllShouldReturnPagedUsers() throws Exception {
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

        PageResponse<UserResponse> response = PageResponse.from(
            new PageImpl<>(List.of(firstResponse, secondResponse)));

        when(adminUserService.findAll(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].firstName").value("Nick"))
            .andExpect(jsonPath("$.content[0].lastName").value("Green"))
            .andExpect(jsonPath("$.content[0].email").value("nick@mail.com"))
            .andExpect(jsonPath("$.content[0].role").value("USER"))
            .andExpect(jsonPath("$.content[1].id").value(2))
            .andExpect(jsonPath("$.content[1].firstName").value("Nora"))
            .andExpect(jsonPath("$.content[1].lastName").value("White"))
            .andExpect(jsonPath("$.content[1].email").value("nora@mail.com"))
            .andExpect(jsonPath("$.content[1].role").value("ADMIN"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        verify(adminUserService).findAll(any(Pageable.class));
    }

    @Test
    void findAllShouldPassPageableToService() throws Exception {
        when(adminUserService.findAll(any(Pageable.class)))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "email,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(adminUserService).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("email")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("email").isDescending()).isTrue();
    }

    @Test
    void findByIdShouldReturnUser() throws Exception {
        UserResponse response = new UserResponse(
            1L,
            "Nick",
            "Green",
            "nick@mail.com",
            Role.USER);

        when(adminUserService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/users/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("Nick"))
            .andExpect(jsonPath("$.lastName").value("Green"))
            .andExpect(jsonPath("$.email").value("nick@mail.com"))
            .andExpect(jsonPath("$.role").value("USER"));

        verify(adminUserService).findById(1L);
    }

    @Test
    void findByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(adminUserService.findById(99L))
                .thenThrow(new NotFoundException("User with id '99' was not found"));

        mockMvc.perform(get("/api/admin/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User with id '99' was not found"))
                .andExpect(jsonPath("$.path").value("/api/admin/users/99"));

        verify(adminUserService).findById(99L);
    }

    @Test
    void updateShouldUpdateUser() throws Exception {
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

        when(adminUserService.update(1L, request)).thenReturn(response);

        mockMvc.perform(put("/api/admin/users/{id}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.email").value("new@mail.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(adminUserService).update(1L, request);
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

        mockMvc.perform(put("/api/admin/users/{id}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/admin/users/1"))
            .andExpect(jsonPath("$.fieldErrors").isArray());

        verify(adminUserService, never()).update(any(), any());
    }

    @Test
    void deleteByIdShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{id}", 1L))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(adminUserService).deleteById(1L);
    }

    @Test
    void deleteByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        doThrow(new NotFoundException("User with id '99' was not found"))
            .when(adminUserService)
            .deleteById(99L);

        mockMvc.perform(delete("/api/admin/users/{id}", 99L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("User with id '99' was not found"))
            .andExpect(jsonPath("$.path").value("/api/admin/users/99"));

        verify(adminUserService).deleteById(99L);
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return validator;
    }
}