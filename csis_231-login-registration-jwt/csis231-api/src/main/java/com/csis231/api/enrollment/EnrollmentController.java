package com.csis231.api.enrollment;

import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints for managing course enrollments.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final UserRepository userRepository;

    @PostMapping("/enrollments/enroll")
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request,
                                     Authentication authentication) {
        User actor = resolveUser(authentication);
        return EnrollmentMapper.toDto(enrollmentService.enroll(actor, request));
    }

    @GetMapping("/students/{userId}/enrollments")
    public List<EnrollmentResponse> enrollmentsForStudent(@PathVariable Long userId,
                                                          Authentication authentication) {
        User actor = resolveUser(authentication);
        if (!actor.getId().equals(userId) && actor.getRole() != User.Role.ADMIN && actor.getRole() != User.Role.INSTRUCTOR) {
            throw new UnauthorizedException("You cannot view enrollments for another student");
        }
        return enrollmentService.findByStudent(userId).stream()
                .map(EnrollmentMapper::toDto)
                .collect(Collectors.toList());
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
