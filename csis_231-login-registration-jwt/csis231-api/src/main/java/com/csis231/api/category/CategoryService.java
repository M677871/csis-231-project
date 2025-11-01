package com.csis231.api.category;
import com.csis231.api.category.Category;
import com.csis231.api.category.CategoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repo;
    public CategoryService(CategoryRepository repo) { this.repo = repo; }

    public List<Category> list() { return repo.findAll(); }

    public Category get(Long id) { return repo.findById(id).orElseThrow(); }

    public Category create(Category c) {
        if (repo.existsByName(c.getName())) throw new IllegalArgumentException("Duplicate category");
        return repo.save(c);
    }

    public Category update(Long id, Category updated) {
        Category c = repo.findById(id).orElseThrow();
        c.setName(updated.getName());
        return repo.save(c);
    }

    public void delete(Long id) { repo.deleteById(id); }
}
