package com.example.demo.api;

import com.example.demo.security.TokenStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Low-level HTTP client used by the frontend to communicate with the backend API.
 *
 * <p>This utility centralizes:
 * <ul>
 *   <li>Resolution of the backend base URL from {@link ClientProps}</li>
 *   <li>Shared {@link HttpClient} configuration (timeout, connection settings)</li>
 *   <li>Attaching the {@code Authorization: Bearer ...} header when a token
 *       is present in {@link com.example.demo.security.TokenStore}</li>
 *   <li>A simple retry mechanism for transient network errors</li>
 * </ul>
 * </p>
 *
 * <p>All methods in this class are {@code static} and return the raw
 * {@link HttpResponse} so that higher-level API wrappers can decide how
 * to parse the JSON payload and handle errors.</p>
 */

public final class ApiClient {
    private ApiClient() {}

    private static final int TIMEOUT_SEC = 60;
    private static final int RETRIES = 3;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SEC))
            .build();

    /**
     * Resolves the base URL of the backend from {@link ClientProps}.
     *
     * <p>The method tries a few well-known property keys in order:
     * {@code api.baseUrl}, {@code backend.baseUrl}, then {@code baseUrl} with
     * a default of {@code http://localhost:8080}.</p>
     *
     * <p>Any trailing slash is removed so that callers can safely concatenate
     * relative paths.</p>
     *
     * @return the normalized base URL without trailing slash
     */

    private static String baseUrl() {
        String v = ClientProps.getOr("api.baseUrl", null);
        if (v == null) v = ClientProps.getOr("backend.baseUrl", null);
        if (v == null) v = ClientProps.getOr("baseUrl", "http://localhost:8080");
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    /**
     * Executes a {@code GET} request against {@code baseUrl() + path}.
     *
     * <p>The request has a JSON {@code Accept} header and, if a token is present
     * in {@link com.example.demo.security.TokenStore}, an
     * {@code Authorization: Bearer ...} header is also added.</p>
     *
     * @param path relative path starting with {@code /} (for example {@code "/api/csis-users"})
     * @return the raw {@link HttpResponse} from the backend
     * @throws Exception if the request fails even after retrying
     */

    public static HttpResponse<String> get(String path) throws Exception {
        String url = baseUrl() + path;
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                .header("Accept", "application/json")
                .GET();
        if (TokenStore.hasToken()) b.header("Authorization", "Bearer " + TokenStore.get());
        return sendWithRetry(url, b.build());
    }

    /**
     * Executes a {@code POST} request with a JSON payload against
     * {@code baseUrl() + path}.
     *
     * <p>The request includes {@code Content-Type: application/json} and
     * {@code Accept: application/json} headers, and appends an
     * {@code Authorization: Bearer ...} header when a token is available.</p>
     *
     * @param path relative path starting with {@code /}
     * @param json serialized JSON request body
     * @return the raw {@link HttpResponse} from the backend
     * @throws Exception if the request fails even after retrying
     */

    public static HttpResponse<String> post(String path, String json) throws Exception {
        String url = baseUrl() + path;
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (TokenStore.hasToken()) b.header("Authorization", "Bearer " + TokenStore.get());
        return sendWithRetry(url, b.build());
    }

    /**
     * Sends the given HTTP request and retries it up to {@link #RETRIES} times
     * if a low-level exception is thrown by the {@link HttpClient}.
     *
     * <p>Between attempts, a small linear backoff is applied
     * ({@code 200ms}, {@code 400ms}, ...).</p>
     *
     * @param url the fully-resolved URL, used only for error reporting
     * @param req the {@link HttpRequest} to send
     * @return the received {@link HttpResponse}
     * @throws RuntimeException if all retry attempts fail
     */

    private static HttpResponse<String> sendWithRetry(String url, HttpRequest req) throws Exception {
        Exception last = null;
        for (int i = 1; i <= RETRIES; i++) {
            try {
                return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                last = e;
                // small backoff
                Thread.sleep(i * 200L);
            }
        }
        throw new RuntimeException("Cannot connect to " + url + " after " + RETRIES + " attempts → "
                + last.getClass().getSimpleName(), last);
    }
}
