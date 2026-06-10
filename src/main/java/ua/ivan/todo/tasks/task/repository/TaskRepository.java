package ua.ivan.todo.tasks.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.ivan.todo.tasks.task.model.Task;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);
}