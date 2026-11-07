package com.marketplace.repositories;

import com.marketplace.entities.Review;
import com.marketplace.entities.ReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy vấn dữ liệu bảng tbl_reviews phục vụ đánh giá hai chiều (FR-15, FR-24).
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByJobIdAndReviewerIdAndRevieweeId(Long jobId, Long reviewerId, Long revieweeId);

    @EntityGraph(attributePaths = {"job", "reviewer", "reviewee"})
    List<Review> findByRevieweeIdAndStatusOrderByCreatedAtDesc(Long revieweeId, ReviewStatus status);

    @EntityGraph(attributePaths = {"job", "reviewer", "reviewee"})
    List<Review> findByJobIdAndReviewerId(Long jobId, Long reviewerId);
}

