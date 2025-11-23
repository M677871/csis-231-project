package com.example.demo.admin;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.common.PagedResponse;
import com.example.demo.model.Category;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * API client for category management.
 */
public class CategoryApi {
    private final ApiClient client = new ApiClient();

    public PagedResponse<Category> list(int page, int size) {
        String path = "/api/categories?page=" + page + "&size=" + size;
        ApiResponse<PagedResponse<Category>> resp = client.get(path, new TypeReference<PagedResponse<Category>>() {});
        return resp.getBody();
    }

    public Category create(String name) {
        Category body = new Category();
        body.setName(name);
        return client.post("/api/categories", body, new TypeReference<Category>() {}).getBody();
    }

    public Category update(Long id, String name) {
        Category body = new Category();
        body.setName(name);
        return client.put("/api/categories/" + id, body, new TypeReference<Category>() {}).getBody();
    }

    public void delete(Long id) {
        client.delete("/api/categories/" + id);
    }
}
