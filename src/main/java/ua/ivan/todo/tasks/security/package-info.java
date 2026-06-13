@ApplicationModule(
    allowedDependencies = {
        "common",
        "user::UserReadFacade",
        "user::UserServiceFacade",
        "user::model",
        "user::UserResponse",
        "user::UserRegistrationRequest"})
package ua.ivan.todo.tasks.security;

import org.springframework.modulith.ApplicationModule;