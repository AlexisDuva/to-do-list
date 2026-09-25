package com.alex.todolist.dto;

import com.alex.todolist.entity.Priority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record TaskRequest(
        @NotBlank String title,
        String description,
        LocalDate dueDate,
        Priority priority,
        Long projectId,
        List<Long> tagIds
) {
}
