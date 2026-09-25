package com.alex.todolist.service;

import com.alex.todolist.dto.TagRequest;
import com.alex.todolist.entity.Tag;
import com.alex.todolist.exception.ResourceNotFoundException;
import com.alex.todolist.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository);
    }

    @Test
    void getAll_returnsAllTagsMappedToResponses() {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("errand");
        when(tagRepository.findAll()).thenReturn(List.of(tag));

        var result = tagService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("errand");
    }

    @Test
    void getById_throwsResourceNotFoundException_whenTagMissing() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesTagWithGivenName() {
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = tagService.create(new TagRequest("urgent"));

        assertThat(response.name()).isEqualTo("urgent");
    }

    @Test
    void delete_removesTag_relyingOnDbCascadeForTaskTagRows() {
        Tag tag = new Tag();
        tag.setId(1L);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        tagService.delete(1L);

        verify(tagRepository).delete(tag);
    }
}
