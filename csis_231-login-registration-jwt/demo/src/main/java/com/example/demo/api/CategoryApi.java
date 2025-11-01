


package com.example.demo.api;


import com.example.demo.model.Category;
import com.example.demo.security.TokenStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class CategoryApi {
    private static final String BASE_URL = "http://localhost:8080/api/categories";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // GET /api/categories
    public List<Category> list() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Authorization", "Bearer " + TokenStore.get())
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(resp.body(), new TypeReference<List<Category>>(){});
    }

    // POST /api/categories
    public Category create(String name) throws IOException, InterruptedException {
        Category body = new Category();
        body.setName(name);
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Authorization", "Bearer " + TokenStore.get())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(resp.body(), Category.class);
    }

    // PUT /api/categories/{id}
    public Category update(Long id, String name) throws IOException, InterruptedException {
        Category body = new Category();
        body.setName(name);
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .header("Authorization", "Bearer " + TokenStore.get())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(resp.body(), Category.class);
    }

    // DELETE /api/categories/{id}
    public void delete(Long id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .header("Authorization", "Bearer " + TokenStore.get())
                .DELETE()
                .build();
        httpClient.send(req, HttpResponse.BodyHandlers.discarding());
    }
}
