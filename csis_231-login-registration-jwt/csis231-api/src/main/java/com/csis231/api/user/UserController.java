package com.csis231.api.user;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing CRUD endpoints for {@link User} entities.
 * The base path /api/csis-users avoids clashing with the standard
 * LearnOnline user controller already present in the application.
 */
@RestController
@RequestMapping("/api/csis-users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Lists all users. */
    @GetMapping
    public Page<User> list(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than zero");
        }
        return userService.getUsers(PageRequest.of(Math.max(0, page), size));
    }

    /** Retrieves a user by ID. */
    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.getUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /** Creates a new user. Expects a complete User in the body. */
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(201).body(created);
    }

    /** Updates an existing user. Only non-null fields will be updated. */
    @PutMapping("/{id}")
    public User update(@PathVariable Long id,
                                       @Valid @RequestBody User user) {
        return userService.updateUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /** Deletes a user by ID. Returns 204 on success or 404 if not found. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns profile information about the currently authenticated user.
     *
     * <p>The username is taken from the {@link Authentication} object and the
     * corresponding {@link User} is mapped to a {@link com.csis231.api.user.MeResponse}
     * DTO that is safe to expose to the frontend.</p>
     *
     * @param authentication the Spring Security authentication of the current request
     * @return a {@link com.csis231.api.user.MeResponse} containing basic user data
     */

    @GetMapping("/me")
    public com.csis231.api.user.MeResponse me(Authentication authentication) {
        String username = authentication.getName();
        User u = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        return new com.csis231.api.user.MeResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhone(),
                u.getRole().name()
        );
    }


}

