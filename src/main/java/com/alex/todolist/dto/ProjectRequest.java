package com.alex.todolist.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(@NotBlank String name) {
}
