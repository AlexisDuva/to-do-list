package com.alex.todolist.service;

import com.alex.todolist.dto.TagRequest;
import com.alex.todolist.dto.TagResponse;
import com.alex.todolist.entity.Tag;
import com.alex.todolist.exception.ResourceNotFoundException;
import com.alex.todolist.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagResponse> getAll() {
        return tagRepository.findAll().stream()
                .map(TagResponse::fromEntity)
                .toList();
    }

    public TagResponse getById(Long id) {
        return TagResponse.fromEntity(findTagOrThrow(id));
    }

    public TagResponse create(TagRequest request) {
        Tag tag = new Tag();
        tag.setName(request.name());
        return TagResponse.fromEntity(tagRepository.save(tag));
    }

    public TagResponse update(Long id, TagRequest request) {
        Tag tag = findTagOrThrow(id);
        tag.setName(request.name());
        return TagResponse.fromEntity(tagRepository.save(tag));
    }

    public void delete(Long id) {
        Tag tag = findTagOrThrow(id);
        // task_tag rows are removed automatically (ON DELETE CASCADE, see V1 migration).
        tagRepository.delete(tag);
    }

    private Tag findTagOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag with id " + id + " not found"));
    }
}
