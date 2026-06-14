@ApplicationModule(
    allowedDependencies = {
        "common",
        "user::model",
        "user::UserReadFacade",
        "user::UserShortResponse",
        "user::UserApiMapper"
    })
package ua.ivan.todo.tasks.task;

import org.springframework.modulith.ApplicationModule;