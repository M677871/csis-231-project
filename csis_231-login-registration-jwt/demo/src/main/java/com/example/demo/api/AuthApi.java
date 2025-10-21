package com.example.demo.api;

import com.example.demo.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpResponse;

public final class AuthApi {
    private AuthApi() {}

    private static final ObjectMapper M = new ObjectMapper();

    private static String path(String key, String def) {
        return ClientProps.getOr(key, def);
    }

    public static AuthResponse login(LoginRequest req) throws Exception {
        String body = M.writeValueAsString(req);
        HttpResponse<String> res = ApiClient.post(path("auth.login", "/api/auth/login"), body);
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
        return parseAuthResponse(res.body());
    }

    // Resend or send OTP code (backend-specific; we send just {"username":...})
    public static void requestOtp(String username) throws Exception {
        ObjectNode n = M.createObjectNode();
        n.put("username", username);
        HttpResponse<String> res = ApiClient.post(path("auth.otp.resend", "/api/auth/otp/request"),
                M.writeValueAsString(n));
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
    }

    public static AuthResponse verifyOtp(OtpVerifyRequest req) throws Exception {
        String body = M.writeValueAsString(req);
        HttpResponse<String> res = ApiClient.post(path("auth.otp", "/api/auth/otp/verify"), body);
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
        return parseAuthResponse(res.body());
    }

    public static void register(RegisterRequest req) throws Exception {
        String body = M.writeValueAsString(req);
        HttpResponse<String> res = ApiClient.post(path("auth.register", "/api/auth/register"), body);
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
    }


    public static void requestResetOtp(String email) throws Exception {
        ObjectNode n = M.createObjectNode();
        n.put("email", email == null ? "" : email.trim());

        HttpResponse<String> res = ApiClient.post(
                path("auth.password.forgot", "/api/auth/password/forgot"),
                M.writeValueAsString(n)
        );
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
    }


    public static void resetWithCode(String emailOrUser, String code, String newPassword) throws Exception {
        ObjectNode n = M.createObjectNode();

        // If user typed an email, send 'email'; if they typed a username, also send 'username' (some backends require email only)
        if (emailOrUser != null && emailOrUser.contains("@")) {
            n.put("email", emailOrUser.trim());
        } else {
            n.put("email", emailOrUser == null ? "" : emailOrUser.trim()); // keep the key 'email' to match your Postman
            n.put("username", emailOrUser == null ? "" : emailOrUser.trim()); // fallback if server accepts username
        }

        n.put("code", code == null ? "" : code.trim());
        n.put("newPassword", newPassword == null ? "" : newPassword.trim());

        HttpResponse<String> res = ApiClient.post(path("auth.reset", "/api/auth/password/reset"),
                M.writeValueAsString(n));
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
    }



    public static MeResponse me() throws Exception {
        HttpResponse<String> res = ApiClient.get(path("me", "/api/me"));
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
        JsonNode n = M.readTree(res.body());
        if (n.hasNonNull("data")) n = n.get("data");     // handle wrapped responses
        return M.treeToValue(n, MeResponse.class);       // tolerant mapping
    }


    // ------- helpers -------

    // Accepts token under many names + accepts {"status":"OTP_REQUIRED"}
    private static AuthResponse parseAuthResponse(String json) throws Exception {
        JsonNode n = M.readTree(json);

        String token = pickString(n, "token", "accessToken", "jwt", "id_token", "access_token");
        boolean otpRequired = pickBoolean(n, "otpRequired", "otp_required", "requiresOtp", "otp");

        if (n.hasNonNull("status")) {
            String s = n.get("status").asText();
            if (s != null && s.equalsIgnoreCase("OTP_REQUIRED")) otpRequired = true;
        }

        if ((token == null || token.isBlank()) && n.hasNonNull("data")) {
            JsonNode d = n.get("data");
            if (d.hasNonNull("status")) {
                String s = d.get("status").asText();
                if (s != null && s.equalsIgnoreCase("OTP_REQUIRED")) otpRequired = true;
            }
            if (token == null) {
                token = pickString(d, "token", "accessToken", "jwt", "id_token", "access_token");
            }
        }

        // If OTP is required, token may legitimately be absent
        if (token == null && !otpRequired) {
            throw new RuntimeException("Login: could not find token in response: " + json);
        }
        return new AuthResponse( otpRequired , token);
    }

    private static String pickString(JsonNode n, String... keys) {
        for (String k : keys) if (n.hasNonNull(k)) return n.get(k).asText();
        return null;
    }

    private static boolean pickBoolean(JsonNode n, String... keys) {
        for (String k : keys) if (n.has(k)) return n.get(k).asBoolean();
        return false;
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "<empty body>" : s;
    }
}
