package ua.ivan.todo.tasks.common.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RestCallLoggingFilterTest {

    private final RestCallLoggingFilter filter = new RestCallLoggingFilter();

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(RestCallLoggingFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void shouldLogRequestAndResponseAndCopyBodyToResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("""
            {
              "email": "user@mail.com",
              "password": "secret"
            }
            """.getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            servletResponse.getWriter().write("""
                {
                  "accessToken": "jwt-token"
                }
                """);
        };

        filter.doFilter(request, response, filterChain);

        assertThat(response.getContentAsString()).contains("jwt-token");

        String logs = logs();

        assertThat(logs).contains("REST request started");
        assertThat(logs).contains("REST request body");
        assertThat(logs).contains("REST response");
        assertThat(logs).contains("\"password\": \"***\"");
        assertThat(logs).doesNotContain("secret");
    }

    @Test
    void shouldLogErrorResponseWithMaskedSensitiveData() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("""
            {
              "email": "user@mail.com",
              "password": "secret"
            }
            """.getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

            servletRequest.getInputStream().readAllBytes();

            httpResponse.setStatus(401);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write("""
                {
                  "accessToken": "jwt-token"
                }
                """);
        };

        filter.doFilter(request, response, filterChain);

        String logs = logs();

        assertThat(logs).contains("REST response error");
        assertThat(logs).contains("\"accessToken\": \"***\"");
        assertThat(logs).doesNotContain("jwt-token");

        assertThat(listAppender.list)
            .anyMatch(event -> event.getLevel().equals(Level.WARN));
    }

    @Test
    void shouldNotLogSwaggerEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (servletRequest, servletResponse) -> servletResponse.getWriter().write("{}");

        filter.doFilter(request, response, filterChain);

        assertThat(logs()).doesNotContain("REST request started");
    }

    private String logs() {
        StringBuilder builder = new StringBuilder();

        for (ILoggingEvent event : listAppender.list) {
            builder.append(event.getFormattedMessage()).append('\n');
        }

        return builder.toString();
    }
}