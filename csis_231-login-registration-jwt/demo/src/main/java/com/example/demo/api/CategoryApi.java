
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

/**
 * API client for managing {@link com.example.demo.model.Category} entities
 * from the frontend.
 *
 * <p>Wraps calls to the backend's {@code /api/categories} endpoints and
 * handles JSON (de)serialization. Authentication is performed by attaching
 * the JWT from {@link com.example.demo.security.TokenStore}.</p>
 */

public class CategoryApi {
    private static final String BASE_URL = "http://localhost:8080/api/categories";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Retrieves all categories from the backend.
     *
     * @return list of all categories
     * @throws IOException          if a low-level I/O error occurs
     * @throws InterruptedException if the HTTP call is interrupted
     */

    public List<Category> list() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Authorization", "Bearer " + TokenStore.get())
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(resp.body(), new TypeReference<List<Category>>(){});
    }

    /**
     * Creates a new category with the given name.
     *
     * @param name the category name to create
     * @return the persisted {@link Category} returned by the backend
     * @throws IOException          if a low-level I/O error occurs
     * @throws InterruptedException if the HTTP call is interrupted
     */

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

    /**
     * Updates the name of an existing category.
     *
     * @param id   identifier of the category to update
     * @param name new category name
     * @return the updated {@link Category} returned by the backend
     * @throws IOException          if a low-level I/O error occurs
     * @throws InterruptedException if the HTTP call is interrupted
     */

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

    /**
     * Deletes a category by its identifier.
     *
     * @param id identifier of the category to delete
     * @throws IOException          if a low-level I/O error occurs
     * @throws InterruptedException if the HTTP call is interrupted
     */

    public void delete(Long id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .header("Authorization", "Bearer " + TokenStore.get())
                .DELETE()
                .build();
        httpClient.send(req, HttpResponse.BodyHandlers.discarding());
    }
}
