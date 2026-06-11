package ua.ivan.todo.tasks.common.dto.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void fromShouldCreatePageResponseFromPage() {
        Page<String> page = new PageImpl<>(
            List.of("first", "second"),
            PageRequest.of(1, 2),
            5);

        PageResponse<String> actual = PageResponse.from(page);

        assertThat(actual.content()).containsExactly("first", "second");
        assertThat(actual.page()).isEqualTo(1);
        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual.totalElements()).isEqualTo(5);
        assertThat(actual.totalPages()).isEqualTo(3);
        assertThat(actual.first()).isFalse();
        assertThat(actual.last()).isFalse();
    }
}