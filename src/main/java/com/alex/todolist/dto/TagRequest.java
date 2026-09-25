package com.alex.todolist.dto;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(@NotBlank String name) {
}
