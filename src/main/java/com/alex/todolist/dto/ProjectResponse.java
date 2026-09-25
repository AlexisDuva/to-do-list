package com.alex.todolist.dto;

import com.alex.todolist.entity.Project;

public record ProjectResponse(Long id, String name) {

    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(project.getId(), project.getName());
    }
}
