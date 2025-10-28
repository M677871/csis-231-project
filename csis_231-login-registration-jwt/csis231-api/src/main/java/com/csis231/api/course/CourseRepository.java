package com.csis231.api.course;

import com.csis231.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByPublishedTrue();
    List<Course> findByInstructor(User instructor);
}
