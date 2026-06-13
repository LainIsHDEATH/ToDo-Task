package ua.ivan.todo.tasks.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.ivan.todo.tasks.common.filter.TransactionIdFilter.TRANSACTION_ID;
import static ua.ivan.todo.tasks.common.filter.TransactionIdFilter.TRANSACTION_ID_HEADER;

class TransactionIdFilterTest {

    private final TransactionIdFilter filter = new TransactionIdFilter();

    @Test
    void shouldUseExistingTransactionIdHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(TRANSACTION_ID_HEADER, "test-transaction-id");

        FilterChain filterChain =
            (servletRequest, servletResponse) -> assertThat(MDC.get(TRANSACTION_ID)).isEqualTo("test-transaction-id");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TRANSACTION_ID_HEADER)).isEqualTo("test-transaction-id");
        assertThat(MDC.get(TRANSACTION_ID)).isNull();
    }

    @Test
    void shouldGenerateTransactionIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (servletRequest, servletResponse) -> assertThat(MDC.get(TRANSACTION_ID)).isNotBlank();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TRANSACTION_ID_HEADER)).isNotBlank();
        assertThat(MDC.get(TRANSACTION_ID)).isNull();
    }
}