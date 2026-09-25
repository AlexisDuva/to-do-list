package com.alex.todolist.controller;

import com.alex.todolist.AbstractIntegrationTest;
import com.alex.todolist.dto.ErrorResponse;
import com.alex.todolist.dto.ProjectRequest;
import com.alex.todolist.dto.ProjectResponse;
import com.alex.todolist.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProjectControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/projects";
    }

    @AfterEach
    void cleanUp() {
        projectRepository.deleteAll();
    }

    @Test
    void createGetUpdateDelete_happyPath() {
        ResponseEntity<ProjectResponse> created = restTemplate.postForEntity(
                baseUrl(), new ProjectRequest("Work"), ProjectResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = created.getBody().id();

        ResponseEntity<ProjectResponse> fetched = restTemplate.getForEntity(
                baseUrl() + "/" + id, ProjectResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().name()).isEqualTo("Work");

        restTemplate.put(baseUrl() + "/" + id, new ProjectRequest("Personal"));
        ResponseEntity<ProjectResponse> updated = restTemplate.getForEntity(
                baseUrl() + "/" + id, ProjectResponse.class);
        assertThat(updated.getBody().name()).isEqualTo("Personal");

        restTemplate.delete(baseUrl() + "/" + id);
        ResponseEntity<ErrorResponse> afterDelete = restTemplate.getForEntity(
                baseUrl() + "/" + id, ErrorResponse.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getById_returns404_whenProjectMissing() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                baseUrl() + "/999999", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void create_returns400_whenNameBlank() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl(), new ProjectRequest(""), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("name");
    }
}
