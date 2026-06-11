package ua.ivan.todo.tasks.user.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.ivan.todo.tasks.task.model.Task;
import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.task.repository.TaskRepository;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.enabled=true"
})
class UserDeletionCascadeTest {

    private static final String COUNT_COLLABORATOR_LINKS_BY_TASK_ID_QUERY = """
            SELECT count(*)
            FROM task_collaborators
            WHERE task_id = ?
            """;

    private static final String COUNT_COLLABORATOR_LINKS_BY_USER_ID_QUERY = """
            SELECT count(*)
            FROM task_collaborators
            WHERE user_id = ?
            """;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deleteUserShouldDeleteOwnedTasksAndCollaboratorLinks() {
        User owner = saveUser("Owner", "User", "owner.cascade@mail.com");
        User collaborator = saveUser("Collaborator", "User", "collaborator.cascade@mail.com");

        Task task = createTask("Task #1", owner);
        task.setCollaborators(Set.of(collaborator));

        Task savedTask = taskRepository.saveAndFlush(task);

        Long taskLinksBeforeDelete = countCollaboratorLinksByTaskId(savedTask.getId());

        entityManager.clear();

        userRepository.deleteById(owner.getId());
        userRepository.flush();

        entityManager.clear();

        Long taskLinksAfterDelete = countCollaboratorLinksByTaskId(savedTask.getId());

        assertThat(taskLinksBeforeDelete).isEqualTo(1L);
        assertThat(taskRepository.existsById(savedTask.getId())).isFalse();
        assertThat(taskLinksAfterDelete).isZero();
        assertThat(userRepository.existsById(collaborator.getId())).isTrue();
    }

    @Test
    void deleteCollaboratorOnlyUserShouldRemoveCollaboratorLinksAndKeepTask() {
        User owner = saveUser("Owner", "User", "owner.only@mail.com");
        User collaborator = saveUser("Collaborator", "User", "collaborator.only@mail.com");

        Task task = createTask("Task #1", owner);
        task.setCollaborators(Set.of(collaborator));

        Task savedTask = taskRepository.saveAndFlush(task);

        Long taskLinksBeforeDelete = countCollaboratorLinksByUserId(collaborator.getId());

        entityManager.clear();

        userRepository.deleteById(collaborator.getId());
        userRepository.flush();

        entityManager.clear();

        Long taskLinksAfterDelete = countCollaboratorLinksByUserId(collaborator.getId());

        assertThat(taskLinksBeforeDelete).isEqualTo(1L);
        assertThat(taskLinksAfterDelete).isZero();
        assertThat(taskRepository.existsById(savedTask.getId())).isTrue();
        assertThat(userRepository.existsById(owner.getId())).isTrue();
    }

    private Long countCollaboratorLinksByTaskId(Long taskId) {
        return jdbcTemplate.queryForObject(
                COUNT_COLLABORATOR_LINKS_BY_TASK_ID_QUERY,
                Long.class,
                taskId
        );
    }

    private Long countCollaboratorLinksByUserId(Long userId) {
        return jdbcTemplate.queryForObject(
                COUNT_COLLABORATOR_LINKS_BY_USER_ID_QUERY,
                Long.class,
                userId
        );
    }

    private User saveUser(String firstName, String lastName, String email) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash("hashed-password")
                .role(Role.USER)
                .build();

        return userRepository.saveAndFlush(user);
    }

    private Task createTask(String name, User owner) {
        return Task.builder()
                .name(name)
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .owner(owner)
                .build();
    }
}