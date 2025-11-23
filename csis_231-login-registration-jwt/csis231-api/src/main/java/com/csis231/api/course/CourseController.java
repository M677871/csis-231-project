package com.csis231.api.course;

import com.csis231.api.common.PagedResponse;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing course catalog and management endpoints.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    @GetMapping
    public PagedResponse<CourseDto> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        var springPage = courseService.listPublished(categoryId, search, pageable)
                .map(CourseMapper::toDto);
        return PagedResponse.fromPage(springPage);
    }

    @GetMapping("/{id}")
    public CourseDetailDto detail(@PathVariable Long id, Authentication authentication) {
        User viewer = currentUserOrNull(authentication);
        return courseService.getCourseDetail(id, viewer);
    }

    @PostMapping
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CourseRequest request,
                                            Authentication authentication) {
        User actor = resolveUser(authentication);
        Course created = courseService.createCourse(request, actor);
        return ResponseEntity.status(201).body(CourseMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public CourseDto update(@PathVariable Long id,
                            @Valid @RequestBody CourseRequest request,
                            Authentication authentication) {
        User actor = resolveUser(authentication);
        Course updated = courseService.updateCourse(id, request, actor);
        return CourseMapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User actor = resolveUser(authentication);
        courseService.deleteCourse(id, actor);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private User currentUserOrNull(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
