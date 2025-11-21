package com.csis231.api.category;

import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller exposing CRUD endpoints for {@link Category} entities.
 *
 * <p>Base path: {@code /api/categories}</p>
 */

@RestController
@RequestMapping("/api/categories")
public class CategoryController
{
    private final CategoryService svc;

    /**
     * Creates a new {@code CategoryController} with the given service.
     *
     * @param svc the {@link CategoryService} to delegate to
     */

    public CategoryController(CategoryService svc)
    {
        this.svc = svc;
    }

    /**
     * Lists all categories.
     *
     * @return list of all {@link Category} entities
     */

    @GetMapping public List<Category> list()
    {
        return svc.list();
    }

    /**
     * Retrieves a single category by its identifier.
     *
     * @param id the ID of the category
     * @return the matching {@link Category}
     */

    @GetMapping("/{id}") public Category get(@PathVariable Long id)
    {
        return svc.get(id);
    }

    /**
     * Creates a new category.
     *
     * @param category the category to create
     * @return the persisted {@link Category}
     */

    @PostMapping public Category create(@RequestBody Category category)
    {
        return svc.create(category);
    }

    /**
     * Updates an existing category.
     *
     * @param id       the ID of the category to update
     * @param category an object containing the new name
     * @return the updated {@link Category}
     */

    @PutMapping("/{id}") public Category update(@PathVariable Long id, @RequestBody Category category)
    {
        return svc.update(id, category);
    }

    /**
     * Deletes a category by its identifier.
     *
     * @param id the ID of the category to delete
     */

    @DeleteMapping("/{id}") public void delete(@PathVariable Long id)
    {
        svc.delete(id);
    }
}
