package com.csis231.api.category;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 1. visible for instructors (for course creation form)
    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ResponseEntity<List<CategoryService.CategoryDto>> list() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    // 2. ADMIN creates new category
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryService.CategoryDto> create(
            @RequestBody CategoryService.CategoryCreateRequest req
    ) {
        return ResponseEntity.ok(categoryService.create(req.name()));
    }

    // 3. ADMIN renames a category
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryService.CategoryDto> update(
            @PathVariable Long id,
            @RequestBody CategoryService.CategoryUpdateRequest req
    ) {
        return ResponseEntity.ok(categoryService.update(id, req.name()));
    }

    // 4. ADMIN deletes a category
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
