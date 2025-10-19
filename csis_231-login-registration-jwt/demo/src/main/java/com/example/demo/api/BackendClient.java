package com.example.demo.api;

import com.example.demo.model.Customer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Properties;

public class BackendClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private ObjectMapper mapper = new ObjectMapper();
    private String authToken = null;

    private final String baseUrl;

    public BackendClient(){
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/client.properties"));
            this.baseUrl = props.getProperty("backend.baseUrl", "http://localhost:8080");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public void clearAuthToken() {
        this.authToken = null;
    }

    private HttpRequest.Builder createRequestBuilder(String uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + uri));
        
        if (authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        
        return builder;
    }

    public List<Customer> fetchCustomers() throws IOException, InterruptedException {
        HttpRequest req = createRequestBuilder("/api/customers")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new RuntimeException("Authentication required. Please login first.");
        }
        
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch customers. Status: " + resp.statusCode());
        }

        return mapper.readValue(resp.body(), new TypeReference<>(){});
    }

    public Customer getCustomer(Long id) throws IOException, InterruptedException {
        HttpRequest req = createRequestBuilder("/api/customers/" + id)
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new RuntimeException("Authentication required. Please login first.");
        }
        
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed to get customer. Status: " + resp.statusCode());
        }

        return mapper.readValue(resp.body(), Customer.class);
    }

    public Customer createCustomer(Customer customer) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(customer);
        HttpRequest req = createRequestBuilder("/api/customers")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new RuntimeException("Authentication required. Please login first.");
        }
        
        if (resp.statusCode() != 201) {
            throw new RuntimeException("Failed to create customer. Status: " + resp.statusCode());
        }

        return mapper.readValue(resp.body(), Customer.class);
    }

    public Customer updateCustomer(Long id, Customer customer) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(customer);
        HttpRequest req = createRequestBuilder("/api/customers/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new RuntimeException("Authentication required. Please login first.");
        }
        
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed to update customer. Status: " + resp.statusCode());
        }

        return mapper.readValue(resp.body(), Customer.class);
    }

    public void deleteCustomer(Long id) throws IOException, InterruptedException {
        HttpRequest req = createRequestBuilder("/api/customers/" + id)
                .DELETE()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new RuntimeException("Authentication required. Please login first.");
        }
        
        if (resp.statusCode() != 204) {
            throw new RuntimeException("Delete failed with status: " + resp.statusCode());
        }
    }

}
