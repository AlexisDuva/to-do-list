package com.alex.todolist.service;

import com.alex.todolist.dto.ProjectRequest;
import com.alex.todolist.dto.ProjectResponse;
import com.alex.todolist.entity.Project;
import com.alex.todolist.entity.Task;
import com.alex.todolist.exception.ResourceNotFoundException;
import com.alex.todolist.repository.ProjectRepository;
import com.alex.todolist.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    public ProjectResponse getById(Long id) {
        return ProjectResponse.fromEntity(findProjectOrThrow(id));
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        return ProjectResponse.fromEntity(projectRepository.save(project));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findProjectOrThrow(id);
        project.setName(request.name());
        return ProjectResponse.fromEntity(projectRepository.save(project));
    }

    public void delete(Long id) {
        Project project = findProjectOrThrow(id);

        List<Task> tasks = taskRepository.findByProjectId(id);
        for (Task task : tasks) {
            task.setProject(null);
        }
        taskRepository.saveAll(tasks);

        projectRepository.delete(project);
    }

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " not found"));
    }
}
