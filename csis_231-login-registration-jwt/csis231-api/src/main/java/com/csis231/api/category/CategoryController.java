package com.csis231.api.category;
import com.csis231.api.category.Category;
import com.csis231.api.category.CategoryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService svc;
    public CategoryController(CategoryService svc) { this.svc = svc; }

    @GetMapping public List<Category> list() { return svc.list(); }

    @GetMapping("/{id}") public Category get(@PathVariable Long id) { return svc.get(id); }

    @PostMapping public Category create(@RequestBody Category category) { return svc.create(category); }

    @PutMapping("/{id}") public Category update(@PathVariable Long id, @RequestBody Category category) {
        return svc.update(id, category);
    }

    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { svc.delete(id); }
}
