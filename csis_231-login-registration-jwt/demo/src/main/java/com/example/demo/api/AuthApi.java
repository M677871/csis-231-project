package com.example.demo.api;

import com.example.demo.model.*;
import com.example.demo.util.AlertUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpResponse;

/**
 * High-level authentication API used by the frontend to talk to the backend.
 *
 * <p>This class wraps the low-level {@link ApiClient} and exposes
 * higher-level operations such as:</p>
 * <ul>
 *   <li>Username/password login with optional OTP-based 2FA</li>
 *   <li>Resending and verifying OTP codes</li>
 *   <li>User registration</li>
 *   <li>Forgot-password and password reset flows</li>
 *   <li>Fetching the current user's profile via {@code /me}</li>
 * </ul>
 *
 * <p>All methods are {@code static}; the class is not meant to be instantiated.</p>
 */

public final class AuthApi {
    private AuthApi() {}

    private static final ObjectMapper M = new ObjectMapper();

    /**
     * Resolves an endpoint path from configuration with a sensible default.
     *
     * <p>Looks up the provided key in {@link ClientProps} and falls back
     * to the given default value when not present.</p>
     *
     * @param key configuration key (for example {@code "auth.login"})
     * @param def default path (for example {@code "/api/auth/login"})
     * @return the configured path or the default
     */

    private static String path(String key, String def) {
        return ClientProps.getOr(key, def);
    }

    /**
     * Performs a login request using username and password.
     *
     * <p>On success, the backend returns either:</p>
     * <ul>
     *   <li>a token (normal login), or</li>
     *   <li>a payload indicating that an OTP is required before issuing a token.</li>
     * </ul>
     *
     * <p>The response JSON is normalized into an {@link AuthResponse}, which
     * exposes both the token and a flag indicating whether OTP is required.</p>
     *
     * @param req login request containing username and password
     * @return parsed {@link AuthResponse} representing the backend reply
     * @throws RuntimeException if the HTTP status code is not 2xx
     */

    public static AuthResponse login(LoginRequest req) throws Exception {
        String body = M.writeValueAsString(req);
        HttpResponse<String> res = ApiClient.post(path("auth.login", "/api/auth/login"), body);
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
        return parseAuthResponse(res.body());
    }

    /**
     * Requests (or resends) an OTP code for the given username.
     *
     * <p>This typically calls the backend's {@code /api/auth/otp/request}
     * endpoint with a small JSON body containing the username.</p>
     *
     * @param username the username or e-mail for which an OTP should be sent
     * @throws RuntimeException if the HTTP status code is not 2xx
     */

    public static void requestOtp(String username) throws Exception {
        ObjectNode n = M.createObjectNode();
        n.put("username", username);
        HttpResponse<String> res = ApiClient.post(path("auth.otp.resend", "/api/auth/otp/request"),
                M.writeValueAsString(n));
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
    }

    /**
     * Verifies an OTP code during the login flow.
     *
     * <p>If the OTP is valid, the backend issues a token which is parsed into
     * an {@link AuthResponse}. If the OTP is invalid or expired, an exception
     * is thrown.</p>
     *
     * @param req request containing username/e-mail and OTP code
     * @return parsed {@link AuthResponse} from the backend
     * @throws RuntimeException if the HTTP status code is not 2xx
     */

    public static AuthResponse verifyOtp(OtpVerifyRequest req) throws Exception {
        String body = M.writeValueAsString(req);
        HttpResponse<String> res = ApiClient.post(path("auth.otp", "/api/auth/otp/verify"), body);
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
        return parseAuthResponse(res.body());
    }

    /**
     * Registers a new user account.
     *
     * <p>On non-2xx HTTP status codes, this method tries to extract a user-friendly
     * error message from the response body and throws a {@link RuntimeException}
     * with that message. Common cases such as {@code 409 Conflict} (duplicate
     * username or e-mail) and {@code 400 Bad Request} are handled with
     * helpful defaults.</p>
     *
     * @param req registration request with username, e-mail and password
     * @throws RuntimeException if registration fails
     */

    public static void register(RegisterRequest req) throws Exception {
        String body = M.writeValueAsString(req);
        HttpResponse<String> res = ApiClient.post(path("auth.register", "/api/auth/register"), body);

        int code = res.statusCode();
        if (code / 100 != 2) {
            String msg = extractMessage(res.body());

            // Friendly defaults for common cases
            if (code == 409) {
                if (msg == null || msg.isBlank()) msg = "Username or email already exists.";
                throw new RuntimeException(msg);
            } else if (code == 400) {
                if (msg == null || msg.isBlank()) msg = "Please check your inputs.";
                throw new RuntimeException(msg);
            }
            throw new RuntimeException("Error (" + code + "): " + (msg == null ? "" : msg));
        }
    }

    private static String extractMessage(String body) {
        try {
            if (body == null || body.isBlank()) return "";
            var n = M.readTree(body);
            if (n.has("message")) return n.get("message").asText();
            if (n.has("error"))   return n.get("error").asText();
            if (n.isTextual())    return n.asText();
            // sometimes servers return a list of validation messages:
            if (n.has("errors") && n.get("errors").isArray() && n.get("errors").size() > 0) {
                return n.get("errors").get(0).asText();
            }
        } catch (Exception ignore) {}
        return body == null ? "" : body;
    }

    /**
     * Starts the "forgot password" flow by requesting a password-reset OTP.
     *
     * @param email e-mail address associated with the account
     * @throws RuntimeException if the HTTP status code is not 2xx
     */

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

    /**
     * Completes the password reset flow using an OTP code and a new password.
     *
     * @param email       e-mail address associated with the account
     * @param code        password-reset OTP code
     * @param newPassword new password to be stored server-side
     * @throws RuntimeException if the HTTP status code is not 2xx
     */

    public static void resetWithCode(String email, String code, String newPassword) throws Exception {
        ObjectNode n = M.createObjectNode();
        n.put("email", email == null ? "" : email.trim());
        n.put("code", code == null ? "" : code.trim());
        n.put("newPassword", newPassword == null ? "" : newPassword.trim());

        HttpResponse<String> res = ApiClient.post(
                path("auth.reset", "/api/auth/password/reset"),
                M.writeValueAsString(n)
        );
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
    }


    /**
     * Fetches information about the currently authenticated user.
     *
     * <p>The method calls the backend {@code /me} endpoint and maps the JSON
     * response to a {@link MeResponse}. It transparently handles responses
     * that wrap the data in a {@code {"data": ...}} object.</p>
     *
     * @return a {@link MeResponse} describing the current user
     * @throws RuntimeException if the HTTP status code is not 2xx
     */


    public static MeResponse me() throws Exception {
        HttpResponse<String> res = ApiClient.get(path("me", "/api/csis-users/me"));
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " - " + safe(res.body()));
        }
        JsonNode n = M.readTree(res.body());
        if (n.hasNonNull("data")) n = n.get("data");     // handle wrapped responses
        return M.treeToValue(n, MeResponse.class);       // tolerant mapping
    }


    // ------- helpers -------

    /**
     * Parses a login/OTP response JSON into an {@link AuthResponse}.
     *
     * <p>The method is tolerant of different backend conventions. It:</p>
     * <ul>
     *   <li>accepts tokens under several field names
     *       ({@code token}, {@code accessToken}, {@code jwt}, {@code id_token}, {@code access_token})</li>
     *   <li>detects OTP-required status via boolean flags or a {@code status}
     *       field that may contain {@code "OTP_REQUIRED"}</li>
     *   <li>also checks a nested {@code data} object when present</li>
     * </ul>
     *
     * @param json raw JSON response body
     * @return normalized {@link AuthResponse} with token and OTP flag
     * @throws RuntimeException if no token can be found and OTP is not required
     */

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
