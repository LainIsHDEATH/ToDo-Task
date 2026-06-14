package ua.ivan.todo.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureVerificationTest {

    @Test
    void shouldVerifyApplicationModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(ToDoTasksApplication.class);

        modules.verify();
    }
}