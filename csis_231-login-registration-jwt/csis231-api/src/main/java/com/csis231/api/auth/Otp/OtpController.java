package com.csis231.api.auth.Otp;

import com.csis231.api.auth.AuthResponse;
import com.csis231.api.auth.JwtUtil;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // OPTIONAL: if you have a DTO with only username, you can also accept Map<String,String>
    @PostMapping("/otp/request")
    public ResponseEntity<?> request(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
        }
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // fixed: use createAndSend, and use fixed purpose LOGIN_2FA
        otpService.createAndSend(u, "LOGIN_2FA");
        return ResponseEntity.accepted().body(Map.of("status", "OTP_SENT"));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody OtpVerifyRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // fixed: no getPurpose() – we use a constant for login flow
        String purpose = "LOGIN_2FA";

        if (otpService.verify(user, purpose, req.getCode())) {
            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthResponse(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired code"));
    }
}
