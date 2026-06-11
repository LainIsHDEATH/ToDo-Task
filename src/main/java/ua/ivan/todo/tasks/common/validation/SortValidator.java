package ua.ivan.todo.tasks.common.validation;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;
import ua.ivan.todo.tasks.common.exception.exceptions.BadRequestException;

import java.util.Set;

@UtilityClass
public final class SortValidator {

    public static void validate(Sort sort, Set<String> allowedSortFields) {
        sort.forEach(order -> {
            String property = order.getProperty();

            if (!allowedSortFields.contains(property)) {
                throw new BadRequestException("Invalid sort field '%s'".formatted(property));
            }
        });
    }
}