package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // ignore unexpected fields
public class MeResponse {

    // Accept many possible names
    @JsonProperty("fullName")   private String fullName;
    @JsonProperty("name")       private String name;
    @JsonProperty("username")   private String username;

    @JsonProperty("email")         private String email;
    @JsonProperty("emailAddress")  private String emailAddress;

    @JsonProperty("role")   private String role;
    @JsonProperty("roles")  private List<String> roles;

    // ---- tolerant accessors used by the UI ----
    public String fullName() {
        if (notBlank(fullName)) return fullName;
        if (notBlank(name))     return name;
        if (notBlank(username)) return username;
        return "";
    }

    public String email() {
        if (notBlank(email))        return email;
        if (notBlank(emailAddress)) return emailAddress;
        return "";
    }

    public String role() {
        if (notBlank(role)) return role;
        if (roles != null && !roles.isEmpty()) return String.join(", ", roles);
        return "";
    }

    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }
}
