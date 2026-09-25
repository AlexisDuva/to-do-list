package com.alex.todolist.service;

import com.alex.todolist.dto.TaskRequest;
import com.alex.todolist.entity.Priority;
import com.alex.todolist.entity.Task;
import com.alex.todolist.exception.ResourceNotFoundException;
import com.alex.todolist.repository.ProjectRepository;
import com.alex.todolist.repository.TagRepository;
import com.alex.todolist.repository.TaskRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TagRepository tagRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectRepository, tagRepository);
    }

    @Test
    void create_defaultsPriorityToMedium_whenNotProvided() {
        when(tagRepository.findAllById(any())).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new TaskRequest("Buy groceries", null, null, null, null, List.of());
        var response = taskService.create(request);

        assertThat(response.priority()).isEqualTo(Priority.MEDIUM);
        assertThat(response.completed()).isFalse();
    }

    @Test
    void create_keepsExplicitPriority_whenProvided() {
        when(tagRepository.findAllById(any())).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new TaskRequest("Buy groceries", null, null, Priority.HIGH, null, List.of());
        var response = taskService.create(request);

        assertThat(response.priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void complete_setsCompletedTrue() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Buy groceries");
        task.setPriority(Priority.MEDIUM);
        task.setCompleted(false);
        task.setTags(List.of());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskService.complete(1L);

        assertThat(response.completed()).isTrue();
    }

    @Test
    void incomplete_setsCompletedFalse() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Buy groceries");
        task.setPriority(Priority.MEDIUM);
        task.setCompleted(true);
        task.setTags(List.of());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskService.incomplete(1L);

        assertThat(response.completed()).isFalse();
    }

    @Test
    void getById_throwsResourceNotFoundException_whenTaskMissing() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }
}
