package com.marketplace.repositories;

import com.marketplace.entities.Review;
import com.marketplace.entities.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu bảng tbl_reviews phục vụ đánh giá hai chiều và kiểm duyệt Admin (FR-15, FR-24, FR-30).
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByJobIdAndReviewerIdAndRevieweeId(Long jobId, Long reviewerId, Long revieweeId);

    @EntityGraph(attributePaths = {"job", "reviewer", "reviewee"})
    List<Review> findByRevieweeIdAndStatusOrderByCreatedAtDesc(Long revieweeId, ReviewStatus status);

    @EntityGraph(attributePaths = {"job", "reviewer", "reviewee"})
    List<Review> findByJobIdAndReviewerId(Long jobId, Long reviewerId);

    @EntityGraph(attributePaths = {"job", "reviewer", "reviewee"})
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"job", "reviewer", "reviewee"})
    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    @Query(
            "SELECT r FROM Review r " +
            "JOIN FETCH r.job " +
            "JOIN FETCH r.reviewer " +
            "JOIN FETCH r.reviewee " +
            "WHERE r.id = :id"
    )
    Optional<Review> findWithDetailsById(@Param("id") Long id);

    long countByStatus(ReviewStatus status);
}

