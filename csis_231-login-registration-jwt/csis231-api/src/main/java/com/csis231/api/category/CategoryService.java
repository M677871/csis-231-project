package com.csis231.api.category;

import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service layer for managing {@link Category} entities.
 *
 * <p>Provides simple CRUD operations and enforces the unique category
 * name constraint.</p>
 */
@Service
public class CategoryService {

    private final CategoryRepository repo;

    /**
     * Creates a new {@code CategoryService} with the given repository.
     *
     * @param repo the repository used to access categories
     */
    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    /**
     * Returns all categories.
     *
     * @return list of all {@link Category} entities
     */
    public List<Category> list() {
        return repo.findAll();
    }

    /**
     * Retrieves a category by its identifier.
     *
     * @param id the category ID
     * @return the matching {@link Category}
     * @throws java.util.NoSuchElementException if no category with this ID exists
     */
    public Category get(Long id) {
        return repo.findById(id).orElseThrow();
    }

    /**
     * Creates a new category.
     *
     * @param c the category to create
     * @return the persisted {@link Category}
     * @throws IllegalArgumentException if a category with the same name already exists
     */
    public Category create(Category c) {
        if (repo.existsByName(c.getName())) {
            throw new IllegalArgumentException("Duplicate category");
        }
        return repo.save(c);
    }

    /**
     * Updates the name of an existing category.
     *
     * @param id      the identifier of the category to update
     * @param updated an object containing the new name
     * @return the updated {@link Category}
     * @throws java.util.NoSuchElementException if no category with this ID exists
     */
    public Category update(Long id, Category updated) {
        Category c = repo.findById(id).orElseThrow();
        c.setName(updated.getName());
        return repo.save(c);
    }

    /**
     * Deletes a category by its identifier.
     *
     * @param id the ID of the category to delete
     */
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
