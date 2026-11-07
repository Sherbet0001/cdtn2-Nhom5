package com.marketplace.services;

import com.marketplace.dtos.CreateReviewRequest;
import com.marketplace.dtos.ReviewResponse;
import com.marketplace.dtos.UserReviewsSummaryResponse;
import com.marketplace.entities.ApplicationStatus;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobStatus;
import com.marketplace.entities.Profile;
import com.marketplace.entities.Review;
import com.marketplace.entities.ReviewStatus;
import com.marketplace.entities.User;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.ApplicationRepository;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.ReviewRepository;
import com.marketplace.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý nghiệp vụ Đánh giá hai chiều giữa Sinh viên và Nhà tuyển dụng sau khi hoàn thành công việc
 * (FR-15, FR-24, QT-REV).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    /**
     * Khởi tạo đánh giá hai chiều sau khi công việc hoàn thành (FR-15: SV đánh giá NTD, FR-24: NTD đánh giá SV).
     * Điều kiện bắt buộc (QT-REV):
     * 1. Tin tuyển dụng phải ở trạng thái COMPLETED (409 nếu chưa COMPLETED).
     * 2. Người đánh giá (reviewer) phải là NTD chủ tin hoặc SV có hồ sơ ACCEPTED tại tin đó;
     *    người được đánh giá (reviewee) phải là phía còn lại.
     * 3. Mỗi cặp (job_id, reviewer_id, reviewee_id) chỉ được đánh giá tối đa 1 lần (409 nếu đánh giá lặp).
     *
     * @param reviewerId ID người gửi đánh giá (đang đăng nhập)
     * @param jobId      ID công việc đã hoàn thành
     * @param request    Dữ liệu đánh giá (revieweeId, rating 1-5, comment)
     * @return Thông tin bản ghi đánh giá vừa tạo với trạng thái mặc định VISIBLE
     * @throws AppException Mã lỗi JOB_NOT_FOUND (404), USER_NOT_FOUND (404), JOB_NOT_COMPLETED (409),
     *                      REVIEW_NOT_ALLOWED (403), hoặc REVIEW_CONFLICT (409)
     */
    @Transactional
    public ReviewResponse reviewUser(Long reviewerId, Long jobId, CreateReviewRequest request) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "Không tìm thấy tài khoản người đánh giá"));

        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        Long revieweeId = request.getRevieweeId();
        User reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "Không tìm thấy tài khoản người được đánh giá"));

        if (reviewerId.equals(revieweeId)) {
            throw new AppException("REVIEW_NOT_ALLOWED", "Bạn không thể tự đánh giá chính mình");
        }

        // QT-REV (FR-15, FR-24): Chỉ cho phép đánh giá khi tin tuyển dụng đã ở trạng thái COMPLETED (409 nếu chưa COMPLETED)
        if (!isJobCompleted(job)) {
            throw new AppException("JOB_NOT_COMPLETED", "Chỉ được phép đánh giá sau khi công việc đã chuyển sang trạng thái hoàn thành (COMPLETED)");
        }

        // QT-REV (FR-15, FR-24): Kiểm tra điều kiện đánh giá hai chiều giữa NTD chủ tin và SV có hồ sơ ACCEPTED
        if (!canUserReviewTargetOnJob(job, reviewerId, revieweeId)) {
            throw new AppException(
                    "REVIEW_NOT_ALLOWED",
                    "Chỉ Nhà tuyển dụng sở hữu tin hoặc Sinh viên có hồ sơ ACCEPTED mới được đánh giá phía còn lại của công việc này"
            );
        }

        // QT-REV (FR-15, FR-24): Ràng buộc duy nhất UNIQUE(job_id, reviewer_id, reviewee_id) — chặn đánh giá lặp (409)
        if (hasAlreadyReviewed(jobId, reviewerId, revieweeId)) {
            throw new AppException("REVIEW_CONFLICT", "Bạn đã gửi đánh giá cho người dùng này đối với công việc này trước đó");
        }

        String trimmedComment = (request.getComment() != null && !request.getComment().trim().isEmpty())
                ? request.getComment().trim()
                : null;

        Review review = Review.builder()
                .job(job)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(trimmedComment)
                .status(ReviewStatus.VISIBLE)
                .build();

        Review savedReview = reviewRepository.save(review);

        Profile reviewerProfile = profileRepository.findByUserId(reviewerId).orElse(null);
        Profile revieweeProfile = profileRepository.findByUserId(revieweeId).orElse(null);

        return mapToReviewResponse(savedReview, reviewerProfile, revieweeProfile);
    }

    /**
     * Lấy danh sách đánh giá ở trạng thái VISIBLE mà một người dùng nhận được kèm điểm trung bình (FR-15, FR-24).
     *
     * @param userId ID người dùng được xem đánh giá
     * @return Đối tượng tổng hợp điểm trung bình (averageRating), tổng số lượt đánh giá và danh sách chi tiết
     * @throws AppException Mã lỗi USER_NOT_FOUND (404) nếu người dùng không tồn tại
     */
    @Transactional(readOnly = true)
    public UserReviewsSummaryResponse getVisibleReviewsForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException("USER_NOT_FOUND", "Không tìm thấy người dùng");
        }

        // QT-REV: Chỉ lấy các bản ghi đánh giá có status = VISIBLE
        List<Review> visibleReviews = reviewRepository.findByRevieweeIdAndStatusOrderByCreatedAtDesc(
                userId,
                ReviewStatus.VISIBLE
        );

        if (visibleReviews.isEmpty()) {
            return UserReviewsSummaryResponse.builder()
                    .userId(userId)
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .reviews(Collections.emptyList())
                    .build();
        }

        Set<Long> userIds = new HashSet<>();
        userIds.add(userId);
        for (Review r : visibleReviews) {
            userIds.add(r.getReviewer().getId());
        }

        Map<Long, Profile> profilesByUserId = profileRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));

        List<ReviewResponse> reviewResponses = visibleReviews.stream()
                .map(r -> mapToReviewResponse(
                        r,
                        profilesByUserId.get(r.getReviewer().getId()),
                        profilesByUserId.get(r.getReviewee().getId())
                ))
                .collect(Collectors.toList());

        double avg = visibleReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        double roundedAvg = Math.round(avg * 10.0) / 10.0;

        return UserReviewsSummaryResponse.builder()
                .userId(userId)
                .averageRating(roundedAvg)
                .totalReviews((long) visibleReviews.size())
                .reviews(reviewResponses)
                .build();
    }

    /**
     * Kiểm tra công việc đã ở trạng thái COMPLETED hay chưa.
     */
    public boolean isJobCompleted(Job job) {
        return job != null && job.getStatus() == JobStatus.COMPLETED;
    }

    /**
     * Kiểm tra cặp (reviewerId, revieweeId) có đủ điều kiện đánh giá hai chiều trên công việc hay không.
     */
    public boolean canUserReviewTargetOnJob(Job job, Long reviewerId, Long revieweeId) {
        Long employerId = job.getEmployer().getId();

        // Trường hợp 1 (FR-24): Người đánh giá là NTD chủ tin -> Người được đánh giá phải là SV có hồ sơ ACCEPTED
        if (employerId.equals(reviewerId)) {
            return applicationRepository.existsByJobIdAndStudentIdAndStatus(
                    job.getId(),
                    revieweeId,
                    ApplicationStatus.ACCEPTED
            );
        }

        // Trường hợp 2 (FR-15): Người được đánh giá là NTD chủ tin -> Người đánh giá phải là SV có hồ sơ ACCEPTED
        if (employerId.equals(revieweeId)) {
            return applicationRepository.existsByJobIdAndStudentIdAndStatus(
                    job.getId(),
                    reviewerId,
                    ApplicationStatus.ACCEPTED
            );
        }

        return false;
    }

    /**
     * Kiểm tra người đánh giá đã từng đánh giá người nhận trong công việc này hay chưa.
     */
    public boolean hasAlreadyReviewed(Long jobId, Long reviewerId, Long revieweeId) {
        return reviewRepository.existsByJobIdAndReviewerIdAndRevieweeId(jobId, reviewerId, revieweeId);
    }

    private ReviewResponse mapToReviewResponse(Review review, Profile reviewerProfile, Profile revieweeProfile) {
        User reviewer = review.getReviewer();
        User reviewee = review.getReviewee();

        String reviewerName = reviewerProfile != null ? reviewerProfile.getFullName() : reviewer.getEmail();
        String revieweeName = revieweeProfile != null ? revieweeProfile.getFullName() : reviewee.getEmail();

        return ReviewResponse.builder()
                .id(review.getId())
                .reviewId(review.getId())
                .jobId(review.getJob().getId())
                .jobTitle(review.getJob().getTitle())
                .reviewerId(reviewer.getId())
                .reviewerName(reviewerName)
                .reviewerRole(reviewer.getRole().name())
                .revieweeId(reviewee.getId())
                .revieweeName(revieweeName)
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus().name())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

