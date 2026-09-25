package com.alex.todolist.dto;

import com.alex.todolist.entity.Tag;

public record TagResponse(Long id, String name) {

    public static TagResponse fromEntity(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
