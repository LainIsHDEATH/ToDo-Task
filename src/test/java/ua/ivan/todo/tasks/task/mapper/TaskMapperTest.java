package ua.ivan.todo.tasks.task.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.ivan.todo.tasks.task.dto.request.TaskCreateRequest;
import ua.ivan.todo.tasks.task.dto.response.TaskListItemResponse;
import ua.ivan.todo.tasks.task.dto.response.TaskResponse;
import ua.ivan.todo.tasks.task.model.Task;
import ua.ivan.todo.tasks.task.model.TaskPriority;
import ua.ivan.todo.tasks.task.model.TaskStatus;
import ua.ivan.todo.tasks.user.mapper.UserMapperImpl;
import ua.ivan.todo.tasks.user.model.Role;
import ua.ivan.todo.tasks.user.model.User;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    TaskMapperImpl.class,
    UserMapperImpl.class
})
class TaskMapperTest {

    @Autowired
    private TaskMapper taskMapper;

    @Test
    void toResponseShouldMapTaskWithOwnerAndCollaborators() {
        User owner = User.builder()
            .id(1L)
            .firstName("Mike")
            .lastName("Brown")
            .email("mike@mail.com")
            .passwordHash("hashed-password")
            .role(Role.USER)
            .build();

        User collaborator = User.builder()
            .id(2L)
            .firstName("Nora")
            .lastName("White")
            .email("nora@mail.com")
            .passwordHash("hashed-password")
            .role(Role.USER)
            .build();

        Task task = Task.builder()
            .id(10L)
            .name("Task #1")
            .priority(TaskPriority.HIGH)
            .status(TaskStatus.IN_PROGRESS)
            .owner(owner)
            .collaborators(Set.of(collaborator))
            .build();

        TaskResponse response = taskMapper.toResponse(task);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Task #1");
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);

        assertThat(response.owner().id()).isEqualTo(1L);
        assertThat(response.owner().email()).isEqualTo("mike@mail.com");

        assertThat(response.collaborators()).hasSize(1);
        assertThat(response.collaborators())
            .extracting("email")
            .containsExactly("nora@mail.com");
    }

    @Test
    void toListItemResponseShouldMapTaskWithoutOwnerAndCollaborators() {
        Task task = Task.builder()
            .id(20L)
            .name("Task #2")
            .priority(TaskPriority.LOW)
            .status(TaskStatus.TODO)
            .build();

        TaskListItemResponse response = taskMapper.toListItemResponse(task);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.name()).isEqualTo("Task #2");
        assertThat(response.priority()).isEqualTo(TaskPriority.LOW);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void toEntityShouldMapCreateRequestWithoutOwnerStatusAndCollaborators() {
        TaskCreateRequest request = new TaskCreateRequest(
            "Task #3",
            TaskPriority.MEDIUM,
            Set.of(1L, 2L));

        Task task = taskMapper.toEntity(request);

        assertThat(task.getId()).isNull();
        assertThat(task.getName()).isEqualTo("Task #3");
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getStatus()).isNull();
        assertThat(task.getOwner()).isNull();
        assertThat(task.getCollaborators()).isEmpty();
    }
}