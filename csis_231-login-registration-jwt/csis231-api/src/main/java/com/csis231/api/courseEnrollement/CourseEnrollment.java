package com.csis231.api.courseEnrollement;

import com.csis231.api.course.Course;
import com.csis231.api.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "course_enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "course_id" })
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student; // must have role == STUDENT

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // or "COMPLETED", etc.

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @PrePersist
    public void onCreate() {
        enrolledAt = Instant.now();
    }
}
