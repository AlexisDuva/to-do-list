package com.alex.todolist.dto;

import com.alex.todolist.entity.Priority;
import com.alex.todolist.entity.Tag;
import com.alex.todolist.entity.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        Priority priority,
        boolean completed,
        LocalDateTime createdAt,
        Long projectId,
        List<Long> tagIds
) {

    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.isCompleted(),
                task.getCreatedAt(),
                task.getProject() != null ? task.getProject().getId() : null,
                task.getTags() != null
                        ? task.getTags().stream().map(Tag::getId).toList()
                        : List.of()
        );
    }
}
