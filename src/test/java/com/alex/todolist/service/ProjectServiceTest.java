package com.alex.todolist.service;

import com.alex.todolist.dto.ProjectRequest;
import com.alex.todolist.entity.Project;
import com.alex.todolist.entity.Task;
import com.alex.todolist.exception.ResourceNotFoundException;
import com.alex.todolist.repository.ProjectRepository;
import com.alex.todolist.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    private ProjectService projectService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, taskRepository);
    }

    @Test
    void getById_throwsResourceNotFoundException_whenProjectMissing() {
        when(projectRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void delete_unassignsTasks_beforeDeletingProject() {
        Project project = new Project();
        project.setId(1L);
        project.setName("Work");

        Task task = new Task();
        task.setId(10L);
        task.setProject(project);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task));

        projectService.delete(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Task>> savedTasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(savedTasksCaptor.capture());
        assertThat(savedTasksCaptor.getValue().get(0).getProject()).isNull();

        verify(projectRepository).delete(project);
    }

    @Test
    void create_savesProjectWithGivenName() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = projectService.create(new ProjectRequest("Personal"));

        assertThat(response.name()).isEqualTo("Personal");
    }
}
