package com.csis231.api.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepo;

    public List<CategoryDto> listAll() {
        return categoryRepo.findAll()
                .stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();
    }

    @Transactional
    public CategoryDto create(String name) {
        Category c = new Category();
        c.setName(name);
        categoryRepo.save(c);
        return new CategoryDto(c.getId(), c.getName());
    }

    @Transactional
    public CategoryDto update(Long id, String name) {
        Category c = categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        c.setName(name);
        categoryRepo.save(c);
        return new CategoryDto(c.getId(), c.getName());
    }

    @Transactional
    public void delete(Long id) {
        categoryRepo.deleteById(id);
    }


    public record CategoryDto(Long id, String name) {}
    public record CategoryCreateRequest(String name) {}
    public record CategoryUpdateRequest(String name) {}
}
