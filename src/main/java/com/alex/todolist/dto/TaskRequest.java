package com.alex.todolist.dto;

import com.alex.todolist.entity.Priority;

import java.time.LocalDate;
import java.util.List;

public record TaskRequest(
        String title,
        String description,
        LocalDate dueDate,
        Priority priority,
        Long projectId,
        List<Long> tagIds
) {
}
