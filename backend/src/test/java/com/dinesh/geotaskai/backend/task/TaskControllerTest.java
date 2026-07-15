package com.dinesh.geotaskai.backend.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TaskControllerTest {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTaskThenListAndReadIt() throws Exception {
        HttpResponse<String> createResponse = post("/api/tasks", validTaskJson("Submit assignment", "College"));

        assertEquals(201, createResponse.statusCode());
        JsonNode createdTask = objectMapper.readTree(createResponse.body());
        assertEquals(1L, createdTask.get("id").asLong());
        assertEquals("Submit assignment", createdTask.get("title").asText());
        assertEquals("College", createdTask.get("locationName").asText());
        assertEquals("Medium", createdTask.get("priority").asText());
        assertFalse(createdTask.get("reminderTriggered").asBoolean());

        HttpResponse<String> listResponse = get("/api/tasks");
        assertEquals(200, listResponse.statusCode());
        JsonNode taskList = objectMapper.readTree(listResponse.body());
        assertEquals(1, taskList.size());
        assertEquals("Submit assignment", taskList.get(0).get("title").asText());

        HttpResponse<String> readResponse = get("/api/tasks/1");
        assertEquals(200, readResponse.statusCode());
        assertEquals("College", objectMapper.readTree(readResponse.body()).get("locationName").asText());
    }

    @Test
    void updateAndMarkReminderTriggered() throws Exception {
        assertEquals(201, post("/api/tasks", validTaskJson("Buy milk", "Store")).statusCode());

        HttpResponse<String> updateResponse = put("/api/tasks/1", validTaskJson("Buy notebooks", "Book shop"));
        assertEquals(200, updateResponse.statusCode());
        JsonNode updatedTask = objectMapper.readTree(updateResponse.body());
        assertEquals("Buy notebooks", updatedTask.get("title").asText());
        assertEquals("Book shop", updatedTask.get("locationName").asText());

        HttpResponse<String> triggeredResponse = patch("/api/tasks/1/reminder-triggered");
        assertEquals(200, triggeredResponse.statusCode());
        assertEquals(true, objectMapper.readTree(triggeredResponse.body()).get("reminderTriggered").asBoolean());
    }

    @Test
    void deleteRemovesTask() throws Exception {
        assertEquals(201, post("/api/tasks", validTaskJson("Call home", "Hostel")).statusCode());

        assertEquals(204, delete("/api/tasks/1").statusCode());
        assertEquals(404, get("/api/tasks/1").statusCode());
    }

    @Test
    void invalidLocationReturnsBadRequest() throws Exception {
        String invalidTask = """
            {
              "title": "Invalid task",
              "notes": "",
              "locationName": "Nowhere",
              "priority": "High",
              "latitude": 100.0,
              "longitude": 77.5946,
              "radiusMeters": 0.0
            }
            """;

        HttpResponse<String> response = post("/api/tasks", invalidTask);

        assertEquals(400, response.statusCode());
        assertEquals("Validation failed", objectMapper.readTree(response.body()).get("message").asText());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).DELETE().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String validTaskJson(String title, String locationName) {
        return """
            {
              "title": "%s",
              "notes": "Created from backend test",
              "locationName": "%s",
              "priority": "Medium",
              "latitude": 12.9716,
              "longitude": 77.5946,
              "radiusMeters": 200.0
            }
            """.formatted(title, locationName);
    }
}
