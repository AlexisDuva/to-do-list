package com.alex.todolist.controller;

import com.alex.todolist.dto.TagRequest;
import com.alex.todolist.dto.TagResponse;
import com.alex.todolist.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> getAll() {
        return tagService.getAll();
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@RequestBody TagRequest request) {
        return ResponseEntity.status(201).body(tagService.create(request));
    }

    @PutMapping("/{id}")
    public TagResponse update(@PathVariable Long id, @RequestBody TagRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
