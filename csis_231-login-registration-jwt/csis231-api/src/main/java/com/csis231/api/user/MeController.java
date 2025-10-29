package com.csis231.api.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public MeDto me(Authentication auth) {
        String principal = auth.getName(); // this is username from JWT

        User user = userRepository.findByUsername(principal)
                .orElseGet(() -> userRepository.findByEmail(principal)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        return new MeDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name() // "STUDENT" | "INSTRUCTOR" | "ADMIN"
        );
    }

    public record MeDto(
            Long id,
            String username,
            String firstName,
            String lastName,
            String role
    ) {}
}
