package com.csis231.api.auth;

import com.csis231.api.auth.Otp.OtpRequiredException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (OtpRequiredException e) {
            // Client should now call /api/auth/otp/verify with {username, code}
            return ResponseEntity.ok(
                    java.util.Map.of("status", "OTP_REQUIRED", "username", e.getUsername())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }
    }


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        return authService.validateToken(token)
                ? ResponseEntity.ok("Token is valid")
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }

    // --- Local exception handlers for cleaner responses ---

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> onBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> onConflict(IllegalStateException ex) {
        // e.g., thrown by register() when username/email already exists
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> onGeneric(Exception ex) {
        log.warn("Auth error", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request");
    }
    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.requestPasswordReset(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok().build();
    }

}
