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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;


import java.util.Map;

/**
 * REST controller exposing authentication-related endpoints for the
 * online learning platform.
 *
 * <p>Provides operations for login (with optional OTP-based 2FA),
 * user registration and password reset via e-mail OTP codes.</p>
 */

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates a user using username and password.
     *
     * <p>If the account requires OTP for login, this method triggers
     * a {@code LOGIN_2FA} OTP and the controller returns an HTTP
     * {@code 202 Accepted} response with {@code otpRequired=true} instead
     * of a token. Otherwise it returns a normal {@link AuthResponse}
     * containing a JWT.</p>
     *
     * @param request the login request containing username and password
     * @return {@code 200 OK} with {@link AuthResponse} when login succeeds
     *         without OTP; {@code 202 Accepted} with an OTP payload when
     *         OTP is required; {@code 401 Unauthorized} on invalid
     *         credentials; {@code 500 Internal Server Error} on unexpected
     *         errors
     */

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

    /**
     * Registers a new user account in the online learning platform.
     *
     * <p>The method enforces uniqueness of username and e-mail, encodes
     * the password and persists a new {@link User} entity.</p>
     *
     * @param req the registration data (username, e-mail, password and
     *            optional profile fields)
     * @return {@code 200 OK} when registration succeeds;
     *         {@code 409 Conflict} if username or e-mail already exist;
     *         {@code 400 Bad Request} or {@code 500 Internal Server Error}
     *         when registration fails for another reason
     */

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

    /**
     * Initiates the "forgot password" flow by issuing a PASSWORD_RESET OTP.
     *
     * <p>An e-mail containing a {@code PASSWORD_RESET} OTP is sent to
     * the supplied address if it belongs to a known user.</p>
     *
     * @param req request body containing the user's e-mail address
     * @return {@code 200 OK} if the reset OTP has been sent;
     *         {@code 401 Unauthorized} if the e-mail is unknown;
     *         {@code 400 Bad Request} if the reset request cannot be processed
     */

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

    /**
     * Completes the password reset flow by verifying the OTP and updating
     * the stored password.
     *
     * @param req request body containing e-mail, reset OTP code and the
     *            new password
     * @return {@code 200 OK} when the password is successfully updated;
     *         {@code 401 Unauthorized} if e-mail or code are invalid;
     *         {@code 400 Bad Request} if the password cannot be reset
     */

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req)
    {
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
