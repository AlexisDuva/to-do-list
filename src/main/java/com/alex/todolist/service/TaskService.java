package com.alex.todolist.service;

import com.alex.todolist.dto.TaskRequest;
import com.alex.todolist.dto.TaskResponse;
import com.alex.todolist.entity.Priority;
import com.alex.todolist.entity.Project;
import com.alex.todolist.entity.Tag;
import com.alex.todolist.entity.Task;
import com.alex.todolist.exception.ResourceNotFoundException;
import com.alex.todolist.repository.ProjectRepository;
import com.alex.todolist.repository.TagRepository;
import com.alex.todolist.repository.TaskRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, TagRepository tagRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
    }

    public List<TaskResponse> getAll(String status, Long projectId, Long tagId, Priority priority,
                                      String search, String sortBy, String sortDir) {
        // Spring Data 4's Specification.where(null) is ambiguous between two overloads
        // (Specification vs PredicateSpecification), so start from an always-true predicate instead.
        Specification<Task> spec = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            boolean completed = status.equals("completed");
            spec = spec.and((root, query, cb) -> cb.equal(root.get("completed"), completed));
        }
        if (projectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
        }
        if (tagId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("tags").get("id"), tagId);
            });
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        Sort sort = buildSort(sortBy, sortDir);

        return taskRepository.findAll(spec, sort).stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    public TaskResponse getById(Long id) {
        return TaskResponse.fromEntity(findTaskOrThrow(id));
    }

    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        applyRequest(task, request);
        task.setCompleted(false);
        task.setCreatedAt(LocalDateTime.now());
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findTaskOrThrow(id);
        applyRequest(task, request);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public TaskResponse complete(Long id) {
        Task task = findTaskOrThrow(id);
        task.setCompleted(true);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public TaskResponse incomplete(Long id) {
        Task task = findTaskOrThrow(id);
        task.setCompleted(false);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    public void delete(Long id) {
        taskRepository.delete(findTaskOrThrow(id));
    }

    private void applyRequest(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);

        if (request.projectId() != null) {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project with id " + request.projectId() + " not found"));
            task.setProject(project);
        } else {
            task.setProject(null);
        }

        if (request.tagIds() != null) {
            List<Tag> tags = new ArrayList<>(tagRepository.findAllById(request.tagIds()));
            task.setTags(tags);
        } else {
            task.setTags(List.of());
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String property = switch (sortBy != null ? sortBy : "createdAt") {
            case "dueDate" -> "dueDate";
            case "priority" -> "priority";
            default -> "createdAt";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }
}
