package com.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dtos.ApplyJobRequest;
import com.marketplace.dtos.CreateJobRequest;
import com.marketplace.dtos.CreateReviewRequest;
import com.marketplace.dtos.LoginRequest;
import com.marketplace.dtos.UpdateApplicationStatusRequest;
import com.marketplace.dtos.UpdateJobStatusRequest;
import com.marketplace.entities.Job;
import com.marketplace.entities.Review;
import com.marketplace.entities.ReviewStatus;
import com.marketplace.entities.User;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.NotificationRepository;
import com.marketplace.repositories.ReviewRepository;
import com.marketplace.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp cho Sprint 4:
 * - FR-23: Đổi trạng thái công việc (OPEN -> FILLED -> COMPLETED, OPEN/FILLED -> CLOSED) + thông báo JOB_COMPLETED
 * - FR-15 & FR-24: Đánh giá hai chiều giữa Sinh viên và Nhà tuyển dụng + xem danh sách đánh giá VISIBLE kèm điểm trung bình
 * - FR-13 & FR-14: Xem danh sách hồ sơ đã nộp (GET /applications/mine?status=...) và chi tiết hồ sơ (GET /applications/{id})
 * - FR-06: Hệ thống thông báo (GET /notifications, unread-count, PATCH read, PATCH read-all)
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class Sprint4WorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("TC-01 (FR-23, FR-15, FR-24, FR-13, FR-06): Luồng End-to-End Đăng tin -> Ứng tuyển -> Duyệt -> COMPLETED -> Thông báo -> Đánh giá hai chiều")
    void fullWorkflow_FR23_FR15_FR24_FR13_FR06_endToEndLifecycle_succeeds() throws Exception {
        String employerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");
        String studentToken = obtainToken("sv2@vanlanguni.vn", "Abc@12345");

        User employer = userRepository.findByEmail("hr@cafegocpho.vn").orElseThrow();
        User student = userRepository.findByEmail("sv2@vanlanguni.vn").orElseThrow();

        // 1. NTD đăng tin tuyển dụng mới (OPEN)
        CreateJobRequest createJobReq = CreateJobRequest.builder()
                .title("Trợ giảng Tiếng Anh Giao tiếp Ca Tối (Sprint 4 E2E)")
                .description("Hỗ trợ giảng viên nước ngoài quản lý lớp học, chấm bài tập về nhà.")
                .categoryId(3L)
                .locationId(1L)
                .salaryMin(new BigDecimal("45000"))
                .salaryMax(new BigDecimal("60000"))
                .salaryUnit("HOUR")
                .workSchedule("Thứ 2 - Thứ 4 - Thứ 6 (18:00 - 21:00)")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 9, 30))
                .slots(2)
                .build();

        MvcResult jobRes = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createJobReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();

        Long jobId = objectMapper.readTree(jobRes.getResponse().getContentAsString())
                .path("data").path("jobId").asLong();

        // 2. Sinh viên ứng tuyển vào tin đang OPEN -> Tạo thông báo APPLICATION_NEW cho NTD
        ApplyJobRequest applyReq = ApplyJobRequest.builder()
                .coverLetter("Em có IELTS 7.0 và kinh nghiệm trợ giảng 6 tháng.")
                .build();

        MvcResult appRes = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        Long applicationId = objectMapper.readTree(appRes.getResponse().getContentAsString())
                .path("data").path("applicationId").asLong();

        // 3. NTD kiểm tra số thông báo chưa đọc (GET /notifications/unread-count) và danh sách (GET /notifications?unreadOnly=true)
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").isNumber());

        MvcResult employerNotifRes = mockMvc.perform(get("/api/v1/notifications")
                        .param("unreadOnly", "true")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].type").value("APPLICATION_NEW"))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andReturn();

        Long employerNotifId = objectMapper.readTree(employerNotifRes.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();

        // NTD đánh dấu 1 thông báo đã đọc (PATCH /notifications/{id}/read)
        mockMvc.perform(patch("/api/v1/notifications/" + employerNotifId + "/read")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));

        // 4. NTD chấp nhận (ACCEPTED) hồ sơ của Sinh viên -> Tạo thông báo APPLICATION_ACCEPTED cho SV
        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationStatusRequest("ACCEPTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // 5. SV kiểm tra danh sách hồ sơ của mình lọc theo status=ACCEPTED (FR-13, FR-14) và chi tiết hồ sơ (GET /applications/{id})
        mockMvc.perform(get("/api/v1/applications/mine")
                        .param("status", "ACCEPTED")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + applicationId + ")].status").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.jobId").value(jobId));

        // 6. NTD đổi trạng thái tin: OPEN -> FILLED rồi FILLED -> COMPLETED (FR-23)
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("FILLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FILLED"));

        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 7. Kiểm tra SV đã nhận thông báo JOB_COMPLETED và đánh dấu đọc tất cả (PATCH /notifications/read-all)
        mockMvc.perform(get("/api/v1/notifications")
                        .param("unreadOnly", "true")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("JOB_COMPLETED"));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 8. Đánh giá hai chiều sau khi COMPLETED:
        // 8a. NTD đánh giá SV (FR-24)
        CreateReviewRequest employerReviewReq = CreateReviewRequest.builder()
                .revieweeId(student.getId())
                .rating(5)
                .comment("Sinh viên đúng giờ, phát âm chuẩn và hỗ trợ lớp học rất nhiệt tình.")
                .build();

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/reviews")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employerReviewReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("VISIBLE"))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.reviewerId").value(employer.getId()))
                .andExpect(jsonPath("$.data.revieweeId").value(student.getId()));

        // 8b. SV đánh giá NTD (FR-15)
        CreateReviewRequest studentReviewReq = CreateReviewRequest.builder()
                .revieweeId(employer.getId())
                .rating(4)
                .comment("Môi trường làm việc thân thiện, trả lương đúng hạn.")
                .build();

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/reviews")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentReviewReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("VISIBLE"))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.reviewerId").value(student.getId()))
                .andExpect(jsonPath("$.data.revieweeId").value(employer.getId()));

        // 9. Xem danh sách đánh giá VISIBLE và điểm trung bình của SV và NTD (GET /users/{userId}/reviews)
        mockMvc.perform(get("/api/v1/users/" + student.getId() + "/reviews")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(student.getId()))
                .andExpect(jsonPath("$.data.totalReviews").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(5.0))
                .andExpect(jsonPath("$.data.reviews[0].rating").value(5));

        mockMvc.perform(get("/api/v1/users/" + employer.getId() + "/reviews")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(employer.getId()))
                .andExpect(jsonPath("$.data.totalReviews").isNumber())
                .andExpect(jsonPath("$.data.averageRating").isNumber());
    }

    @Test
    @DisplayName("TC-02 (FR-23): Kiểm tra quy tắc trạng thái tin (chặn nhảy cóc OPEN -> COMPLETED và chặn quay ngược từ COMPLETED/CLOSED -> 409)")
    void changeJobStatus_FR23_invalidTransitionsAndTerminalStates_returns409Conflict() throws Exception {
        String employerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");
        Long jobId = createSampleOpenJob(employerToken, "Nhân viên Kiểm kê Kho Cuối tuần");

        // 1. Chặn nhảy cóc OPEN -> COMPLETED (phải qua FILLED trước) -> 409
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("COMPLETED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));

        // 2. Chuyển OPEN -> FILLED thành công (200)
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("FILLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FILLED"));

        // 3. Chặn quay ngược FILLED -> OPEN (409)
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("OPEN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));

        // 4. Chuyển FILLED -> COMPLETED thành công (200)
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 5. COMPLETED là trạng thái cuối: chặn COMPLETED -> CLOSED hoặc COMPLETED -> OPEN (409)
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("CLOSED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));

        // 6. Kiểm tra OPEN -> CLOSED thành công và CLOSED là trạng thái cuối (chặn CLOSED -> OPEN)
        Long secondJobId = createSampleOpenJob(employerToken, "Nhân viên Phát tờ rơi Sự kiện");
        mockMvc.perform(patch("/api/v1/jobs/" + secondJobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("CLOSED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(patch("/api/v1/jobs/" + secondJobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("OPEN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("TC-03 (FR-23): Chặn NTD khác không phải chủ tin hoặc Sinh viên đổi trạng thái tin -> 403 Forbidden")
    void changeJobStatus_FR23_nonOwnerOrStudent_returns403Forbidden() throws Exception {
        String ownerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");
        String otherEmployerToken = obtainToken("tuyendung@eduviet.vn", "Abc@12345");
        String studentToken = obtainToken("sv1@vanlanguni.vn", "Abc@12345");

        Long jobId = createSampleOpenJob(ownerToken, "Nhân viên Thu ngân Ca Chiều");

        // NTD khác đổi trạng thái -> 403
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + otherEmployerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("FILLED"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_RESOURCE"));

        // Sinh viên đổi trạng thái -> 403
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("FILLED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-04 & TC-05 (FR-15, FR-24): Chặn đánh giá khi tin chưa COMPLETED (409) và chặn đánh giá lặp (409)")
    void reviewUser_FR15_FR24_notCompletedOrDuplicateReview_returns409Conflict() throws Exception {
        String employerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");
        String studentToken = obtainToken("sv1@vanlanguni.vn", "Abc@12345");
        User employer = userRepository.findByEmail("hr@cafegocpho.vn").orElseThrow();
        User student = userRepository.findByEmail("sv1@vanlanguni.vn").orElseThrow();

        Long jobId = createSampleOpenJob(employerToken, "Cộng tác viên Chụp ảnh Sản phẩm");

        MvcResult appRes = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplyJobRequest("Em có máy ảnh riêng."))))
                .andExpect(status().isCreated())
                .andReturn();

        Long appId = objectMapper.readTree(appRes.getResponse().getContentAsString())
                .path("data").path("applicationId").asLong();

        mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationStatusRequest("ACCEPTED"))))
                .andExpect(status().isOk());

        // Khi tin còn OPEN -> Đánh giá trả 409 JOB_NOT_COMPLETED
        CreateReviewRequest reviewReq = CreateReviewRequest.builder()
                .revieweeId(employer.getId())
                .rating(5)
                .comment("Rất tốt")
                .build();

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/reviews")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("JOB_NOT_COMPLETED"));

        // Chuyển OPEN -> FILLED -> COMPLETED
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("FILLED"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("COMPLETED"))))
                .andExpect(status().isOk());

        // Đánh giá lần 1 thành công (201)
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/reviews")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isCreated());

        // Đánh giá lặp lần 2 -> 409 REVIEW_CONFLICT
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/reviews")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REVIEW_CONFLICT"));
    }

    @Test
    @DisplayName("TC-06 (FR-15, FR-24): Chặn Sinh viên không có hồ sơ ACCEPTED đánh giá hoặc NTD đánh giá SV chưa ACCEPTED -> 403")
    void reviewUser_FR15_FR24_unacceptedStudent_returns403Forbidden() throws Exception {
        String employerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");
        String rejectedStudentToken = obtainToken("sv2@vanlanguni.vn", "Abc@12345");
        User employer = userRepository.findByEmail("hr@cafegocpho.vn").orElseThrow();
        User rejectedStudent = userRepository.findByEmail("sv2@vanlanguni.vn").orElseThrow();

        Long jobId = createSampleOpenJob(employerToken, "Nhân viên Phụ bếp Cuối tuần");

        MvcResult appRes = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                        .header("Authorization", "Bearer " + rejectedStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplyJobRequest("Em xin ứng tuyển."))))
                .andExpect(status().isCreated())
                .andReturn();

        Long appId = objectMapper.readTree(appRes.getResponse().getContentAsString())
                .path("data").path("applicationId").asLong();

        // NTD từ chối (REJECTED) hồ sơ của sv2
        mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationStatusRequest("REJECTED"))))
                .andExpect(status().isOk());

        // Đổi trạng thái công việc sang FILLED -> COMPLETED
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("FILLED"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateJobStatusRequest("COMPLETED"))))
                .andExpect(status().isOk());

        // SV bị REJECTED cố tình đánh giá NTD -> 403 REVIEW_NOT_ALLOWED
        CreateReviewRequest studentReviewReq = CreateReviewRequest.builder()
                .revieweeId(employer.getId())
                .rating(1)
                .comment("Đánh giá trái phép")
                .build();

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/reviews")
                        .header("Authorization", "Bearer " + rejectedStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentReviewReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_ALLOWED"));

        // NTD cố tình đánh giá SV bị REJECTED -> 403 REVIEW_NOT_ALLOWED
        CreateReviewRequest employerReviewReq = CreateReviewRequest.builder()
                .revieweeId(rejectedStudent.getId())
                .rating(3)
                .comment("Đánh giá SV chưa làm việc")
                .build();

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/reviews")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employerReviewReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("TC-07 (FR-15, FR-24): GET /users/{userId}/reviews chỉ trả đánh giá VISIBLE và không tính đánh giá HIDDEN vào điểm trung bình")
    void getUserReviews_FR15_FR24_excludesHiddenReviewsFromAverage() throws Exception {
        String employerToken = obtainToken("tuyendung@eduviet.vn", "Abc@12345");
        User employer = userRepository.findByEmail("tuyendung@eduviet.vn").orElseThrow();
        User student1 = userRepository.findByEmail("sv1@vanlanguni.vn").orElseThrow();
        User student2 = userRepository.findByEmail("sv2@vanlanguni.vn").orElseThrow();

        Long jobId = createSampleOpenJob(employerToken, "Gia sư Toán Lớp 9");
        Job job = jobRepository.findById(jobId).orElseThrow();

        // Lưu 1 đánh giá VISIBLE (4 sao) và 1 đánh giá HIDDEN (1 sao) cho employer
        Review visibleReview = Review.builder()
                .job(job)
                .reviewer(student1)
                .reviewee(employer)
                .rating(4)
                .comment("Đánh giá hợp lệ hiển thị công khai")
                .status(ReviewStatus.VISIBLE)
                .build();
        reviewRepository.save(visibleReview);

        Review hiddenReview = Review.builder()
                .job(job)
                .reviewer(student2)
                .reviewee(employer)
                .rating(1)
                .comment("Đánh giá vi phạm đã bị ẩn")
                .status(ReviewStatus.HIDDEN)
                .build();
        reviewRepository.save(hiddenReview);

        MvcResult res = mockMvc.perform(get("/api/v1/users/" + employer.getId() + "/reviews")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode dataNode = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        for (JsonNode revNode : dataNode.path("reviews")) {
            assertThat(revNode.path("status").asText()).isEqualTo("VISIBLE");
            assertThat(revNode.path("comment").asText()).isNotEqualTo("Đánh giá vi phạm đã bị ẩn");
        }
    }

    @Test
    @DisplayName("TC-08 (FR-13, FR-06): Bảo vệ quyền sở hữu khi xem chi tiết đơn ứng tuyển và đánh dấu đọc thông báo của người khác -> 403")
    void applicationsAndNotifications_FR13_FR06_crossUserAccess_returns403Forbidden() throws Exception {
        String employerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");
        String student1Token = obtainToken("sv1@vanlanguni.vn", "Abc@12345");
        String student2Token = obtainToken("sv2@vanlanguni.vn", "Abc@12345");

        Long jobId = createSampleOpenJob(employerToken, "Nhân viên Bán hàng Lưu niệm");

        MvcResult appRes = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplyJobRequest("Hồ sơ của SV1"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long appId = objectMapper.readTree(appRes.getResponse().getContentAsString())
                .path("data").path("applicationId").asLong();

        // SV2 cố tình xem chi tiết đơn ứng tuyển của SV1 -> 403 FORBIDDEN_RESOURCE
        mockMvc.perform(get("/api/v1/applications/" + appId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_RESOURCE"));

        // Lấy 1 thông báo của NTD và dùng token của SV2 gọi PATCH /notifications/{id}/read -> 403 FORBIDDEN_RESOURCE
        MvcResult notifRes = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andReturn();

        Long employerNotifId = objectMapper.readTree(notifRes.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/notifications/" + employerNotifId + "/read")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_RESOURCE"));
    }

    private Long createSampleOpenJob(String employerToken, String title) throws Exception {
        CreateJobRequest request = CreateJobRequest.builder()
                .title(title)
                .description("Mô tả chi tiết cho công việc: " + title)
                .categoryId(1L)
                .locationId(1L)
                .salaryMin(new BigDecimal("30000"))
                .salaryMax(new BigDecimal("40000"))
                .salaryUnit("HOUR")
                .workSchedule("Linh hoạt theo ca")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .slots(2)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("jobId").asLong();
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}

