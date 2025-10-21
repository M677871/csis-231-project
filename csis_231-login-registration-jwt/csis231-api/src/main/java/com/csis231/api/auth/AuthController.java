package com.csis231.api.auth;

import com.csis231.api.auth.Otp.OtpRequiredException;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse resp = authService.login(request);
            return ResponseEntity.ok(resp); // no-OTP case (if you ever disable OTP)
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        } catch (OtpRequiredException e) {
            // when OTP is required: 202 (no token)
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                            "otpRequired", true,
                            "purpose", "LOGIN_2FA",
                            "username", e.getUsername()
                    ));
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            if (userRepository.findByUsername(req.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Username already exists"));
            }
            if (userRepository.findByEmail(req.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Email already exists"));
            }

            User u = new User();
            u.setUsername(req.getUsername());
            u.setEmail(req.getEmail());
            u.setPassword(passwordEncoder.encode(req.getPassword()));
            try { u.setFirstName(req.getFirstName()); } catch (Throwable ignore) {}
            try { u.setLastName(req.getLastName()); }  catch (Throwable ignore) {}

            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "Registered"));
        } catch (Exception e) {
            log.error("Register error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Registration failed"));
        }
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            authService.requestPasswordReset(req); // sends PASSWORD_RESET OTP
            return ResponseEntity.ok(Map.of("message", "OTP sent"));
        } catch (BadCredentialsException e) {
            // avoid leaking which emails exist
            return ResponseEntity.ok(Map.of("message", "If the email exists, an OTP has been sent"));
        } catch (Exception e) {
            log.error("Forgot password error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Could not send reset code"));
        }
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            authService.resetPassword(req);
            return ResponseEntity.ok(Map.of("message", "Password updated"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or code"));
        } catch (Exception e) {
            log.error("Reset password error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Could not reset password"));
        }
    }
}
