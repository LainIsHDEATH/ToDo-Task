package ua.ivan.todo.tasks.task.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.ivan.todo.tasks.task.model.Task;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOwnerId(Long ownerId);

    Page<Task> findAllByOwnerId(Long ownerId, Pageable pageable);

    boolean existsByOwnerId(Long ownerId);
}