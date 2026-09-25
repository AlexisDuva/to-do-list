package com.alex.todolist.controller;

import com.alex.todolist.AbstractIntegrationTest;
import com.alex.todolist.dto.ErrorResponse;
import com.alex.todolist.dto.TaskRequest;
import com.alex.todolist.dto.TaskResponse;
import com.alex.todolist.entity.Priority;
import com.alex.todolist.repository.ProjectRepository;
import com.alex.todolist.repository.TagRepository;
import com.alex.todolist.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TaskControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TagRepository tagRepository;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/tasks";
    }

    @AfterEach
    void cleanUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void createCompleteDelete_happyPath() {
        ResponseEntity<TaskResponse> created = restTemplate.postForEntity(
                baseUrl(), new TaskRequest("Buy groceries", null, null, Priority.HIGH, null, List.of()), TaskResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().completed()).isFalse();
        Long id = created.getBody().id();

        ResponseEntity<TaskResponse> completed = restTemplate.exchange(
                baseUrl() + "/" + id + "/complete", org.springframework.http.HttpMethod.PATCH, null, TaskResponse.class);
        assertThat(completed.getBody().completed()).isTrue();

        restTemplate.delete(baseUrl() + "/" + id);
        ResponseEntity<ErrorResponse> afterDelete = restTemplate.getForEntity(
                baseUrl() + "/" + id, ErrorResponse.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAll_filtersByPriority() {
        restTemplate.postForEntity(baseUrl(),
                new TaskRequest("High priority task", null, null, Priority.HIGH, null, List.of()), TaskResponse.class);
        restTemplate.postForEntity(baseUrl(),
                new TaskRequest("Low priority task", null, null, Priority.LOW, null, List.of()), TaskResponse.class);

        ResponseEntity<TaskResponse[]> response = restTemplate.getForEntity(
                baseUrl() + "?priority=HIGH", TaskResponse[].class);

        assertThat(response.getBody()).extracting(TaskResponse::title).containsExactly("High priority task");
    }

    @Test
    void create_returns400_whenTitleBlank() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl(), new TaskRequest("   ", null, null, null, null, List.of()), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("title");
    }

    @Test
    void create_returns404_whenProjectIdUnknown() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl(), new TaskRequest("Task with bad project", null, null, null, 999999L, List.of()), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
