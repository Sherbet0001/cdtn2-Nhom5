package com.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dtos.ApplyJobRequest;
import com.marketplace.dtos.CreateJobRequest;
import com.marketplace.dtos.LoginRequest;
import com.marketplace.dtos.UpdateApplicationStatusRequest;
import com.marketplace.entities.Category;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobStatus;
import com.marketplace.entities.Location;
import com.marketplace.entities.NotificationType;
import com.marketplace.entities.Profile;
import com.marketplace.entities.SalaryUnit;
import com.marketplace.entities.User;
import com.marketplace.repositories.ApplicationRepository;
import com.marketplace.repositories.CategoryRepository;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.LocationRepository;
import com.marketplace.repositories.NotificationRepository;
import com.marketplace.repositories.ProfileRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp cho Sprint 3 (Module Catalog, Jobs & Applications):
 * FR-07, FR-08, FR-09, FR-10, FR-12, FR-17, FR-18, FR-19, FR-20, FR-21, FR-22 và NFR-01 (Benchmark 1.000 tin < 2 giây).
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class JobAndApplicationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    @DisplayName("Catalog Public Endpoints: GET /categories và GET /locations mở công khai không cần JWT")
    void getCatalogEndpoints_publicAccess_returnsCategoriesAndLocations() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(8));

        mockMvc.perform(get("/api/v1/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    @DisplayName("FR-17: Nhà tuyển dụng đăng tin mới thành công trả về 201 Created kèm {jobId, status: OPEN}")
    void createJob_FR17_validEmployerRequest_returnsCreated201WithOpenStatus() throws Exception {
        String employerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");

        CreateJobRequest request = CreateJobRequest.builder()
                .title("Nhân viên Phục vụ Ca Sáng Cuối Tuần")
                .description("Phục vụ khách hàng tại quán cà phê acoustic, dọn dẹp quầy và hỗ trợ pha chế cơ bản.")
                .categoryId(1L)
                .locationId(3L)
                .salaryMin(new BigDecimal("28000"))
                .salaryMax(new BigDecimal("35000"))
                .salaryUnit("HOUR")
                .workSchedule("Thứ 7 - Chủ nhật (07:00 - 12:00)")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .slots(2)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").isNumber())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.title").value("Nhân viên Phục vụ Ca Sáng Cuối Tuần"))
                .andReturn();

        Long createdJobId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("jobId").asLong();
        jobRepository.deleteById(createdJobId);
    }

    @Test
    @DisplayName("FR-17: Đăng tin thất bại khi salaryMin > salaryMax hoặc startDate > endDate (400 BAD REQUEST)")
    void createJob_FR17_invalidSalaryRangeOrDateRange_returnsBadRequest400() throws Exception {
        String employerToken = obtainToken("hr@cafegocpho.vn", "Abc@12345");

        CreateJobRequest invalidSalaryReq = CreateJobRequest.builder()
                .title("Tin sai khoảng lương")
                .description("Mô tả chi tiết công việc kiểm thử lỗi salaryMin lớn hơn salaryMax.")
                .categoryId(1L)
                .locationId(1L)
                .salaryMin(new BigDecimal("50000"))
                .salaryMax(new BigDecimal("30000"))
                .salaryUnit("HOUR")
                .workSchedule("Ca tối")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 1))
                .slots(1)
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSalaryReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("FR-18 & FR-19: NTD xem danh sách tin đã đăng (GET /jobs/mine), cập nhật tin (PUT /jobs/{id}), chặn sửa tin của NTD khác (403) hoặc tin COMPLETED (409)")
    void manageEmployerJobs_FR18_FR19_listMineAndUpdateJob_enforcesOwnershipAndStatusRules() throws Exception {
        String employer1Token = obtainToken("hr@cafegocpho.vn", "Abc@12345");
        String employer2Token = obtainToken("tuyendung@eduviet.vn", "Abc@12345");

        // 1. GET /api/v1/jobs/mine của NTD 1
        mockMvc.perform(get("/api/v1/jobs/mine")
                        .header("Authorization", "Bearer " + employer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());

        // 2. Tạo 1 tin mới của NTD 1 để kiểm thử PUT /api/v1/jobs/{id}
        CreateJobRequest createReq = CreateJobRequest.builder()
                .title("Tin tuyển dụng cần chỉnh sửa")
                .description("Mô tả ban đầu trước khi cập nhật thông tin tuyển dụng.")
                .categoryId(1L)
                .locationId(3L)
                .salaryMin(new BigDecimal("25000"))
                .salaryMax(new BigDecimal("30000"))
                .salaryUnit("HOUR")
                .workSchedule("Thứ 2 - Thứ 6")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 7, 1))
                .slots(3)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long jobId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("jobId").asLong();

        try {
            // 3. NTD 1 cập nhật tin của chính mình -> 200 OK
            CreateJobRequest updateReq = CreateJobRequest.builder()
                    .title("Tin tuyển dụng đã cập nhật lương")
                    .description("Mô tả công việc sau khi cập nhật mức lương và số lượng.")
                    .categoryId(1L)
                    .locationId(3L)
                    .salaryMin(new BigDecimal("30000"))
                    .salaryMax(new BigDecimal("40000"))
                    .salaryUnit("HOUR")
                    .workSchedule("Thứ 2 - Thứ 7")
                    .startDate(LocalDate.of(2026, 5, 1))
                    .endDate(LocalDate.of(2026, 7, 1))
                    .slots(4)
                    .build();

            mockMvc.perform(put("/api/v1/jobs/" + jobId)
                            .header("Authorization", "Bearer " + employer1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Tin tuyển dụng đã cập nhật lương"))
                    .andExpect(jsonPath("$.data.slots").value(4));

            // 4. NTD 2 cố tình sửa tin của NTD 1 -> 403 FORBIDDEN
            mockMvc.perform(put("/api/v1/jobs/" + jobId)
                            .header("Authorization", "Bearer " + employer2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            // 5. Khi tin đã chuyển sang COMPLETED -> không cho phép PUT (409 CONFLICT)
            Job jobEntity = jobRepository.findById(jobId).orElseThrow();
            jobEntity.setStatus(JobStatus.COMPLETED);
            jobRepository.save(jobEntity);

            mockMvc.perform(put("/api/v1/jobs/" + jobId)
                            .header("Authorization", "Bearer " + employer1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
        } finally {
            jobRepository.deleteById(jobId);
        }
    }

    @Test
    @DisplayName("FR-07, FR-08, FR-09, FR-10: Danh sách tin chỉ trả tin OPEN, hỗ trợ tìm kiếm từ khóa, lọc categoryId/locationId/minSalary+salaryUnit và xem chi tiết")
    void searchAndViewJobs_FR07_to_FR10_filtersOnlyOpenJobsAndAppliesSalaryUnitRule() throws Exception {
        // 1. Tìm kiếm theo từ khóa "kiểm thử" -> khớp tin số 4
        mockMvc.perform(get("/api/v1/jobs")
                        .param("keyword", "kiểm thử")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("Thực tập sinh Part-time hỗ trợ kiểm thử Website Giáo dục"));

        // 2. Lọc theo minSalary đi kèm salaryUnit=HOUR với mức 40,000đ/giờ -> chỉ khớp tin Trợ giảng (45k-60k/giờ)
        mockMvc.perform(get("/api/v1/jobs")
                        .param("minSalary", "40000")
                        .param("salaryUnit", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Trợ giảng lớp Lập trình & Tiếng Anh giao tiếp cuối tuần"));

        // 3. Quy tắc nghiệp vụ: Truyền minSalary cực lớn nhưng THIẾU salaryUnit -> hệ thống bỏ qua minSalary và trả về đầy đủ tin OPEN
        mockMvc.perform(get("/api/v1/jobs")
                        .param("minSalary", "999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4));

        // 4. Xem chi tiết tin tuyển dụng GET /api/v1/jobs/1 -> 200 OK
        mockMvc.perform(get("/api/v1/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value(1))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.companyName").isNotEmpty());
    }

    @Test
    @DisplayName("FR-12, FR-20, FR-21, FR-22: Luồng Sinh viên ứng tuyển -> Ghi thông báo NTD -> NTD xem ứng viên -> Duyệt ACCEPTED/REJECTED -> Chặn vượt slots (409)")
    void applicationLifecycle_FR12_FR20_FR21_FR22_applyReviewAndSlotLimitEnforcement() throws Exception {
        String employerToken = obtainToken("tuyendung@eduviet.vn", "Abc@12345");
        String student1Token = obtainToken("sv1@vanlanguni.vn", "Abc@12345");
        String student2Token = obtainToken("sv2@vanlanguni.vn", "Abc@12345");

        // Tạo 1 tin mới có slots = 1 để kiểm thử chặn vượt slots
        CreateJobRequest jobReq = CreateJobRequest.builder()
                .title("Tuyển 01 Tester Kiểm Thử Tự Động")
                .description("Viết kịch bản kiểm thử tích hợp JUnit và kiểm tra giao diện.")
                .categoryId(6L)
                .locationId(5L)
                .salaryMin(new BigDecimal("40000"))
                .salaryMax(new BigDecimal("50000"))
                .salaryUnit("HOUR")
                .workSchedule("Linh hoạt 3 buổi/tuần")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 7, 30))
                .slots(1)
                .build();

        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString())
                .path("data").path("jobId").asLong();

        Long app1Id = null;
        Long app2Id = null;
        try {
            // 1. SV 1 ứng tuyển vào jobId -> 201 Created + tạo thông báo APPLICATION_NEW cho NTD
            ApplyJobRequest apply1 = ApplyJobRequest.builder()
                    .coverLetter("Em đã có kinh nghiệm viết JUnit 5 và MockMvc, mong được nhận việc.")
                    .build();

            MvcResult app1Res = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                            .header("Authorization", "Bearer " + student1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(apply1)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.jobId").value(jobId))
                    .andReturn();
            app1Id = objectMapper.readTree(app1Res.getResponse().getContentAsString())
                    .path("data").path("id").asLong();

            // Kiểm tra thông báo APPLICATION_NEW được ghi vào tbl_notifications
            final Long createdApp1Id = app1Id;
            long notifCountAfterApply = notificationRepository.findAll().stream()
                    .filter(n -> n.getApplication() != null
                            && n.getApplication().getId().equals(createdApp1Id)
                            && n.getType() == NotificationType.APPLICATION_NEW)
                    .count();
            assertThat(notifCountAfterApply).isEqualTo(1L);

            // 2. SV 1 ứng tuyển lặp lần 2 vào cùng jobId -> 409 CONFLICT (APPLICATION_CONFLICT)
            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                            .header("Authorization", "Bearer " + student1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(apply1)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.errorCode").value("APPLICATION_CONFLICT"));

            // 3. SV 2 cũng ứng tuyển vào jobId -> 201 Created
            ApplyJobRequest apply2 = ApplyJobRequest.builder()
                    .coverLetter("Em có kỹ năng kiểm thử giao diện và báo cáo lỗi chi tiết.")
                    .build();

            MvcResult app2Res = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                            .header("Authorization", "Bearer " + student2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(apply2)))
                    .andExpect(status().isCreated())
                    .andReturn();
            app2Id = objectMapper.readTree(app2Res.getResponse().getContentAsString())
                    .path("data").path("id").asLong();

            // 4. FR-20: NTD chủ tin xem danh sách ứng viên GET /api/v1/jobs/{jobId}/applications -> trả về 2 hồ sơ kèm CV
            mockMvc.perform(get("/api/v1/jobs/" + jobId + "/applications")
                            .header("Authorization", "Bearer " + employerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].studentCvContent").isNotEmpty());

            // 5. FR-21: NTD duyệt ACCEPTED cho SV 1 -> 200 OK (acceptedCount = 1/1 slots) + ghi thông báo APPLICATION_ACCEPTED
            UpdateApplicationStatusRequest acceptReq = UpdateApplicationStatusRequest.builder()
                    .status("ACCEPTED")
                    .build();

            mockMvc.perform(patch("/api/v1/applications/" + app1Id + "/status")
                            .header("Authorization", "Bearer " + employerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acceptReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

            final Long finalApp1Id = app1Id;
            long acceptedNotifCount = notificationRepository.findAll().stream()
                    .filter(n -> n.getApplication() != null
                            && n.getApplication().getId().equals(finalApp1Id)
                            && n.getType() == NotificationType.APPLICATION_ACCEPTED)
                    .count();
            assertThat(acceptedNotifCount).isEqualTo(1L);

            // 6. FR-21: NTD cố duyệt ACCEPTED thêm cho SV 2 khi đã đủ 1/1 slots -> Chặn với 409 CONFLICT (SLOTS_EXCEEDED)
            mockMvc.perform(patch("/api/v1/applications/" + app2Id + "/status")
                            .header("Authorization", "Bearer " + employerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acceptReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.errorCode").value("SLOTS_EXCEEDED"));

            // 7. FR-22: NTD từ chối (REJECTED) hồ sơ của SV 2 -> 200 OK + ghi thông báo APPLICATION_REJECTED
            UpdateApplicationStatusRequest rejectReq = UpdateApplicationStatusRequest.builder()
                    .status("REJECTED")
                    .build();

            mockMvc.perform(patch("/api/v1/applications/" + app2Id + "/status")
                            .header("Authorization", "Bearer " + employerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rejectReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"));
        } finally {
            final Long cleanupApp1Id = app1Id;
            final Long cleanupApp2Id = app2Id;
            notificationRepository.deleteAll(
                    notificationRepository.findAll().stream()
                            .filter(n -> n.getApplication() != null
                                    && (n.getApplication().getId().equals(cleanupApp1Id)
                                    || n.getApplication().getId().equals(cleanupApp2Id)))
                            .toList()
            );
            if (app1Id != null) applicationRepository.deleteById(app1Id);
            if (app2Id != null) applicationRepository.deleteById(app2Id);
            jobRepository.deleteById(jobId);
        }
    }

    @Test
    @DisplayName("FR-12: Chặn ứng tuyển khi Sinh viên chưa cập nhật CV trong hồ sơ cá nhân (409 CONFLICT - CV_REQUIRED)")
    void applyForJob_FR12_studentWithoutCv_returnsConflict409() throws Exception {
        User sv3 = userRepository.findByEmail("sv3@vanlanguni.vn").orElseThrow();
        Profile sv3Profile = profileRepository.findByUserId(sv3.getId()).orElseThrow();
        String originalCv = sv3Profile.getCvContent();
        sv3Profile.setCvContent("   ");
        profileRepository.save(sv3Profile);

        try {
            String sv3Token = obtainToken("sv3@vanlanguni.vn", "Abc@12345");
            ApplyJobRequest applyReq = ApplyJobRequest.builder()
                    .coverLetter("Ứng tuyển khi chưa có CV")
                    .build();

            mockMvc.perform(post("/api/v1/jobs/1/applications")
                            .header("Authorization", "Bearer " + sv3Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applyReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.errorCode").value("CV_REQUIRED"));
        } finally {
            sv3Profile.setCvContent(originalCv);
            profileRepository.save(sv3Profile);
        }
    }

    @Test
    @DisplayName("NFR-01 Benchmark: Seed 1.000 tin tuyển dụng OPEN và đo thời gian phản hồi GET /api/v1/jobs < 2.000 ms (2 giây)")
    void benchmarkGetJobs_NFR01_with1000SeededJobs_respondsUnderTwoSeconds() throws Exception {
        User employer = userRepository.findByEmail("hr@cafegocpho.vn").orElseThrow();
        Category category = categoryRepository.findById(1L).orElseThrow();
        Location location = locationRepository.findById(1L).orElseThrow();

        List<Job> batchJobs = new ArrayList<>(1000);
        for (int i = 1; i <= 1000; i++) {
            batchJobs.add(Job.builder()
                    .employer(employer)
                    .category(category)
                    .location(location)
                    .title("Tin tuyển dụng Part-time Sinh viên Benchmark #" + i)
                    .description("Mô tả công việc part-time thực tế phục vụ đo kiểm hiệu năng NFR-01 với 1000 bản ghi #" + i)
                    .salaryMin(new BigDecimal("25000"))
                    .salaryMax(new BigDecimal("40000"))
                    .salaryUnit(SalaryUnit.HOUR)
                    .workSchedule("Ca linh hoạt Sáng / Chiều / Tối")
                    .startDate(LocalDate.of(2026, 5, 1))
                    .endDate(LocalDate.of(2026, 12, 31))
                    .slots(3)
                    .status(JobStatus.OPEN)
                    .build());
        }
        List<Job> savedJobs = jobRepository.saveAll(batchJobs);

        try {
            long totalCount = jobRepository.count();
            assertThat(totalCount).isGreaterThanOrEqualTo(1000L);

            long startTimeNs = System.nanoTime();
            mockMvc.perform(get("/api/v1/jobs")
                            .param("keyword", "Benchmark")
                            .param("categoryId", "1")
                            .param("locationId", "1")
                            .param("minSalary", "25000")
                            .param("salaryUnit", "HOUR")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(1000));
            long elapsedMs = (System.nanoTime() - startTimeNs) / 1_000_000;

            System.out.println("==================================================================");
            System.out.println("[NFR-01 BENCHMARK RESULT] Tong so tin trong DB: " + totalCount
                    + " | Thoi gian phan hoi GET /api/v1/jobs: " + elapsedMs + " ms (Chi tieu < 2000 ms)");
            System.out.println("==================================================================");

            assertThat(elapsedMs).isLessThan(2000L);
        } finally {
            jobRepository.deleteAllInBatch(savedJobs);
        }
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
