package com.Mihaela.taskmanager.security;

import com.Mihaela.taskmanager.entity.Project;
import com.Mihaela.taskmanager.entity.Role;
import com.Mihaela.taskmanager.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {
    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user))
            return null;
        return user;
    }

    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    public boolean isOwner(Project project, User user) {
        return user.getId().equals(project.getOwner().getId());
    }

    public boolean isMember(Project project, User user) {
        return project.getMembers().contains(user);
    }

}
