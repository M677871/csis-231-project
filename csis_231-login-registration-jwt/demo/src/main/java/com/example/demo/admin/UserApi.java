package com.example.demo.admin;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.common.PagedResponse;
import com.example.demo.model.User;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * API client for user management endpoints.
 */
public class UserApi {
    private final ApiClient client = new ApiClient();

    public PagedResponse<User> list(int page, int size) {
        String path = "/api/csis-users?page=" + page + "&size=" + size;
        ApiResponse<PagedResponse<User>> resp = client.get(path, new TypeReference<PagedResponse<User>>() {});
        return resp.getBody();
    }

    public User get(Long id) {
        return client.get("/api/csis-users/" + id, new TypeReference<User>() {}).getBody();
    }

    public User create(User user) {
        return client.post("/api/csis-users", user, new TypeReference<User>() {}).getBody();
    }

    public User update(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id must not be null for update");
        }
        return client.put("/api/csis-users/" + user.getId(), user, new TypeReference<User>() {}).getBody();
    }

    public void delete(Long id) {
        client.delete("/api/csis-users/" + id);
    }
}
