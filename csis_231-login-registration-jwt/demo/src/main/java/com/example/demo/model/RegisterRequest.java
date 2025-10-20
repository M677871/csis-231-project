package com.example.demo.model;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String firstName,
        String lastName
) {}
