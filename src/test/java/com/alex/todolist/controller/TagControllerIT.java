package com.alex.todolist.controller;

import com.alex.todolist.AbstractIntegrationTest;
import com.alex.todolist.dto.ErrorResponse;
import com.alex.todolist.dto.TagRequest;
import com.alex.todolist.dto.TagResponse;
import com.alex.todolist.repository.TagRepository;
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
class TagControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TagRepository tagRepository;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/tags";
    }

    @AfterEach
    void cleanUp() {
        tagRepository.deleteAll();
    }

    @Test
    void createUpdateListDelete_happyPath() {
        ResponseEntity<TagResponse> created = restTemplate.postForEntity(
                baseUrl(), new TagRequest("errand"), TagResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = created.getBody().id();

        ResponseEntity<TagResponse[]> list = restTemplate.getForEntity(baseUrl(), TagResponse[].class);
        assertThat(list.getBody()).extracting(TagResponse::name).contains("errand");

        restTemplate.put(baseUrl() + "/" + id, new TagRequest("urgent"));
        ResponseEntity<TagResponse[]> listAfterUpdate = restTemplate.getForEntity(baseUrl(), TagResponse[].class);
        assertThat(listAfterUpdate.getBody()).extracting(TagResponse::name).contains("urgent");

        restTemplate.delete(baseUrl() + "/" + id);
        ResponseEntity<TagResponse[]> listAfterDelete = restTemplate.getForEntity(baseUrl(), TagResponse[].class);
        assertThat(listAfterDelete.getBody()).isEmpty();
    }

    @Test
    void create_returns400_whenNameMissing() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl(), new TagRequest(null), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("name");
    }
}
