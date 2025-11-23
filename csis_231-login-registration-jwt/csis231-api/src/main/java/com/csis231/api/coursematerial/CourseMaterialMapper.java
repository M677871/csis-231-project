package com.csis231.api.coursematerial;

/**
 * Mapper for course materials.
 */
public final class CourseMaterialMapper {
    private CourseMaterialMapper() {}

    public static CourseMaterialDto toDto(CourseMaterial material) {
        if (material == null) return null;
        return new CourseMaterialDto(
                material.getId(),
                material.getCourse() != null ? material.getCourse().getId() : null,
                material.getTitle(),
                material.getMaterialType(),
                material.getUrl(),
                material.getMetadata(),
                material.getCreatedAt()
        );
    }
}
