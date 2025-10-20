package com.csis231.api.auth;

import com.csis231.api.auth.Otp.OtpPurposes;
import com.csis231.api.auth.Otp.OtpRequiredException;
import com.csis231.api.auth.Otp.OtpService;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired private com.csis231.api.auth.Otp.OtpService otpService;

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Username is already taken!");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email is already in use!");

        User.Role targetRole = User.Role.STUDENT;
        if (request.getRole() != null) {
            try { targetRole = User.Role.valueOf(request.getRole().toUpperCase()); }
            catch (IllegalArgumentException ignored) { /* fallback to STUDENT */ }
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .isActive(true)
                .emailVerified(false)
                .twoFactorEnabled(false)
                .role(targetRole)
                .build();

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getUsername());

        return new AuthResponse(
                token,
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If 2FA enabled: send OTP and signal controller
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            otpService.createAndSend(user, "LOGIN_2FA");
            throw new OtpRequiredException(user.getUsername());
        }

        // No 2FA: issue token now
        String token = jwtUtil.generateToken(userDetails);
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email())
                .ifPresent(otpService::sendPasswordResetOtp);
        // Always return 200 even if email doesn't exist (avoid user enumeration)
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or code"));

        otpService.verifyOtpOrThrow(user, OtpPurposes.PASSWORD_RESET, req.code()); // <-- plural & new method

        user.setPassword(passwordEncoder.encode(req.newPassword()));
    }

        public boolean validateToken(String token) { return jwtUtil.validateToken(token); }

    public String generateTokenFor(String username) {
        return jwtUtil.generateToken(username);
    }

}
