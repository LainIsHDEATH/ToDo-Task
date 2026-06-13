package ua.ivan.todo.tasks.user.api.interfaces;

import org.springframework.modulith.NamedInterface;
import ua.ivan.todo.tasks.user.model.User;
import java.util.Set;
import java.util.List;
import java.util.Optional;

@NamedInterface("UserReadFacade")
public interface UserReadFacade {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> findAllByIds(Set<Long> ids);

    boolean existsById(Long id);
}