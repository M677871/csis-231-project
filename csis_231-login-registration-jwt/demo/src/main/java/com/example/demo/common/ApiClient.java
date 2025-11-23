package com.example.demo.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.common.PageResponse;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared HTTP client that adds auth headers, parses JSON and normalizes errors.
 */
public class ApiClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String baseUrl;

    public ApiClient() {
        String url = ClientProps.getOr("baseUrl", "http://localhost:8080");
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public <T> ApiResponse<T> get(String path, TypeReference<T> typeRef) {
        HttpRequest request = baseRequest(path).GET().build();
        return send(request, typeRef);
    }

    public ApiResponse<String> post(String path, Object payload) {
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();

        return send(request, null);
    }

    public <T> ApiResponse<T> post(String path, Object payload, TypeReference<T> typeRef) {
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();
        return send(request, typeRef);
    }

    public <T> ApiResponse<T> put(String path, Object payload, TypeReference<T> typeRef) {
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();
        return send(request, typeRef);
    }

    public void delete(String path) {
        HttpRequest request = baseRequest(path).DELETE().build();
        send(request, null);
    }

    public <T> T read(String payload, TypeReference<T> typeRef) {
        return parseBody(payload, typeRef);
    }

    private HttpRequest.Builder baseRequest(String path) {
        String url = path.startsWith("http") ? path : baseUrl + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json");
        if (TokenStore.hasToken()) {
            builder.header("Authorization", "Bearer " + TokenStore.get());
        }
        return builder;
    }

    private <T> ApiResponse<T> send(HttpRequest request, TypeReference<T> typeRef) {
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status < 200 || status >= 300) {
                ErrorResponse err = parseBody(response.body(), new TypeReference<ErrorResponse>() {});
                String message = err != null && err.getMessage() != null
                        ? err.getMessage()
                        : "Request failed";
                String code = err != null ? err.getCode() : null;
                throw new ApiException(status, message, code);
            }

            T body = parseBody(response.body(), typeRef);
            return new ApiResponse<>(status, body, response.body());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(0, "Request failed: " + e.getMessage(), null, e);
        }
    }

    private String toJson(Object payload) {
        try {
            if (payload == null) {
                return "{}";
            }
            if (payload instanceof String s) {
                return s;
            }
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ApiException(0, "Could not serialize request body", null, e);
        }
    }

    private <T> T parseBody(String body, TypeReference<T> typeRef) {
        if (typeRef == null || body == null || body.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(body, typeRef);
        } catch (Exception e) {
            // TEMPORARY DEBUG LOGGING
            System.err.println("=== JSON PARSE ERROR ===");
            System.err.println("Target type: " + typeRef.getType());
            System.err.println("Raw response body:");
            System.err.println(body);
            System.err.println("========================");
            throw new ApiException(0, "Could not parse response", null, e);
        }
    }
    /**
     * Helper for paginated endpoints (Page<T> style JSON).
     * It parses the page envelope manually and maps each item to the given itemClass.
     */
    public <T> PageResponse<T> getPage(String path, Class<T> itemClass) {
        HttpRequest request = baseRequest(path)
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String body = response.body();

            if (status < 200 || status >= 300) {
                // Try to parse the standard error shape
                ErrorResponse err = null;
                try {
                    err = MAPPER.readValue(body, ErrorResponse.class);
                } catch (Exception ignore) {
                    // fall back to generic message
                }

                String message = (err != null && err.getMessage() != null)
                        ? err.getMessage()
                        : ("Request failed with HTTP status " + status);
                String code = (err != null ? err.getCode() : null);

                throw new ApiException(status, message, code);
            }

            // ✅ Manually parse the page JSON
            JsonNode root = MAPPER.readTree(body);

            PageResponse<T> page = new PageResponse<>();
            page.setNumber(root.path("number").asInt());
            page.setSize(root.path("size").asInt());
            page.setTotalElements(root.path("totalElements").asLong());
            page.setTotalPages(root.path("totalPages").asInt());
            page.setFirst(root.path("first").asBoolean());
            page.setLast(root.path("last").asBoolean());

            List<T> items = new ArrayList<>();
            JsonNode contentNode = root.path("content");
            if (contentNode.isArray()) {
                for (JsonNode node : contentNode) {
                    T item = MAPPER.treeToValue(node, itemClass);
                    items.add(item);
                }
            }
            page.setContent(items);

            return page;
        } catch (IOException | InterruptedException e) {
            throw new ApiException(0, "Request failed: " + e.getMessage(), null, e);
        } catch (Exception e) {
            throw new ApiException(0, "Could not parse page response", null, e);
        }
    }

}
