package ua.ivan.todo.tasks.task.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ua.ivan.todo.tasks.task.model.Task;
import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;
import ua.ivan.todo.tasks.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findAllByOwnerIdShouldReturnTasksOfConcreteOwner() {
        User owner = saveUser("Mike", "Brown", "mike@mail.com");
        User anotherOwner = saveUser("Nora", "White", "nora@mail.com");

        Task firstTask = createTask("Task #1", owner);
        Task secondTask = createTask("Task #2", owner);
        Task anotherUserTask = createTask("Task #3", anotherOwner);

        taskRepository.saveAll(List.of(firstTask, secondTask, anotherUserTask));

        List<Task> actual = taskRepository.findAllByOwnerId(owner.getId());

        assertThat(actual)
                .hasSize(2)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("Task #1", "Task #2");
    }

    @Test
    void existsByOwnerIdShouldReturnTrueWhenOwnerHasTasks() {
        User owner = saveUser("Nick", "Green", "nick@mail.com");
        Task task = createTask("Task #1", owner);

        taskRepository.save(task);

        boolean exists = taskRepository.existsByOwnerId(owner.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByOwnerIdShouldReturnFalseWhenOwnerHasNoTasks() {
        User owner = saveUser("Nora", "White", "nora.white@mail.com");

        boolean exists = taskRepository.existsByOwnerId(owner.getId());

        assertThat(exists).isFalse();
    }

    private User saveUser(String firstName, String lastName, String email) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash("hashed-password")
                .role(Role.USER)
                .build();

        return userRepository.save(user);
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