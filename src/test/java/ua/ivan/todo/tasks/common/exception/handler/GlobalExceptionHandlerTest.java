package ua.ivan.todo.tasks.common.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;
import ua.ivan.todo.tasks.common.exception.exceptions.ConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.DeleteConflictException;
import ua.ivan.todo.tasks.common.exception.exceptions.NotFoundException;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void invalidRequestBodyShouldReturnValidationErrorResponse() throws Exception {
        String body = """
                {
                  "email": "invalid-email",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(3)));
    }

    @Test
    void constraintViolationShouldReturnValidationErrorResponse() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/test/constraint-violation"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("userId"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("User id must be positive"))
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").value(-1));
    }

    @Test
    void malformedJsonShouldReturnBadRequestErrorResponse() throws Exception {
        String body = """
                {
                  "name": "Task",
                  "priority": "HIGH"
                """;

        mockMvc.perform(post("/test/malformed-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.path").value("/test/malformed-json"));
    }

    @Test
    void invalidEnumValueShouldReturnMalformedJsonErrorResponse() throws Exception {
        String body = """
                {
                  "priority": "CRITICAL"
                }
                """;

        mockMvc.perform(post("/test/invalid-enum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.path").value("/test/invalid-enum"));
    }

    @Test
    void notFoundShouldReturnNotFoundErrorResponse() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"));
    }

    @Test
    void conflictShouldReturnConflictErrorResponse() throws Exception {
        mockMvc.perform(post("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.path").value("/test/conflict"));
    }

    @Test
    void deleteConflictShouldReturnConflictErrorResponse() throws Exception {
        mockMvc.perform(delete("/test/delete-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User cannot be deleted because user owns tasks"))
                .andExpect(jsonPath("$.path").value("/test/delete-conflict"));
    }

    @Test
    void dataIntegrityViolationShouldReturnSafeConflictErrorResponse() throws Exception {
        mockMvc.perform(post("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Data integrity violation"))
                .andExpect(jsonPath("$.path").value("/test/data-integrity"));
    }

    @Test
    void unexpectedExceptionShouldReturnInternalServerErrorWithoutSensitiveMessage() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"))
                .andExpect(jsonPath("$.path").value("/test/unexpected"));
    }

    @RestController
    private static class TestExceptionController {

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody TestValidationRequest request) {
        }

        @GetMapping("/test/constraint-violation")
        void constraintViolation() {
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);

            when(path.toString()).thenReturn("userId");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("User id must be positive");
            when(violation.getInvalidValue()).thenReturn(-1L);

            throw new ConstraintViolationException(Set.of(violation));
        }

        @PostMapping("/test/malformed-json")
        void malformedJson(@RequestBody TestEnumRequest request) {
        }

        @PostMapping("/test/invalid-enum")
        void invalidEnum(@RequestBody TestEnumRequest request) {
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("User not found");
        }

        @PostMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Email already exists");
        }

        @DeleteMapping("/test/delete-conflict")
        void deleteConflict() {
            throw new DeleteConflictException("User cannot be deleted because user owns tasks");
        }

        @PostMapping("/test/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint uk_users_email");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("Sensitive internal details");
        }
    }

    private record TestValidationRequest(

            @NotBlank(message = "Name is required")
            String name,

            @Email(message = "Email must be valid")
            String email,

            @Size(min = 8, message = "Password must be at least 8 characters")
            String password
    ) {
    }

    private record TestEnumRequest(
            TestPriority priority
    ) {
    }

    private enum TestPriority {
        LOW,
        MEDIUM,
        HIGH
    }
}