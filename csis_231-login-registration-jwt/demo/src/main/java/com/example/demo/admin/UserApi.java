package com.example.demo.admin;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResponse;
import com.example.demo.common.PagedResponse;
import com.example.demo.model.User;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * API client for user management endpoints.
 */
public class UserApi {
    private final ApiClient client = new ApiClient();

    public PageResponse<User> list(int page, int size) {
        String path = "/api/csis-users?page=" + page + "&size=" + size;
        return client.getPage(path, User.class);
    }

    /**
     * Finds first user whose username or email matches the given identifier.
     * Scans first 200 results to avoid backend changes.
     */
    public java.util.Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return java.util.Optional.empty();
        try {
            PageResponse<User> page = client.getPage("/api/csis-users?page=0&size=200", User.class);
            if (page != null && page.getContent() != null) {
                return page.getContent().stream()
                        .filter(u -> identifier.equalsIgnoreCase(u.getUsername()) || identifier.equalsIgnoreCase(u.getEmail()))
                        .findFirst();
            }
        } catch (Exception ignored) {}
        return java.util.Optional.empty();
    }

    public java.util.List<User> listInstructors(int pageSize) {
        PageResponse<User> page = client.getPage("/api/csis-users?page=0&size=" + pageSize, User.class);
        if (page != null && page.getContent() != null) {
            return page.getContent().stream()
                    .filter(u -> "INSTRUCTOR".equalsIgnoreCase(u.getRole()))
                    .toList();
        }
        return java.util.List.of();
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
