package com.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dtos.AdminUpdateJobStatusRequest;
import com.marketplace.dtos.CategoryRequest;
import com.marketplace.dtos.CreateReviewRequest;
import com.marketplace.dtos.LocationRequest;
import com.marketplace.dtos.LoginRequest;
import com.marketplace.dtos.UpdateApplicationStatusRequest;
import com.marketplace.dtos.UpdateJobStatusRequest;
import com.marketplace.dtos.UpdateReviewStatusRequest;
import com.marketplace.dtos.UpdateUserStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bộ kiểm thử tích hợp cho toàn bộ chức năng Quản trị hệ thống Sprint 5:
 * FR-14, FR-25, FR-26, FR-27, FR-28, FR-29, FR-30, FR-31.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null")
class Sprint5AdminIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("TC-S5-01 (FR-25, FR-26): Admin xem danh sách người dùng, khóa/mở khóa tài khoản SV và chặn tự khóa chính mình (409)")
    void adminUserManagementAndLockFlow() throws Exception {
        String adminToken = loginAndGetToken("admin@vlu.edu.vn", "Abc@12345");
        String studentToken = loginAndGetToken("sv3@vanlanguni.vn", "Abc@12345");

        // 1. SV truy cập /api/v1/admin/users -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 2. Admin lấy danh sách người dùng lọc theo role=STUDENT
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("role", "STUDENT")
                        .param("keyword", "Bảo")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("sv3@vanlanguni.vn"));

        // 3. Admin tự khóa chính mình (userId=1) -> 409 ADMIN_SELF_LOCK
        UpdateUserStatusRequest lockReq = UpdateUserStatusRequest.builder().status("LOCKED").build();
        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_SELF_LOCK"));

        // 4. Admin khóa tài khoản sv3 (userId=6) -> 200 OK
        mockMvc.perform(patch("/api/v1/admin/users/6/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        // 5. sv3 dùng token cũ gọi API hoặc đăng nhập lại đều bị chặn 403 ACCOUNT_LOCKED
        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));

        // 6. Admin mở khóa lại cho sv3 -> ACTIVE
        UpdateUserStatusRequest unlockReq = UpdateUserStatusRequest.builder().status("ACTIVE").build();
        mockMvc.perform(patch("/api/v1/admin/users/6/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unlockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("TC-S5-02 (FR-27): Admin ẩn tin OPEN/FILLED sang HIDDEN, khôi phục tự động về OPEN (< slots) hoặc FILLED (>= slots), và chặn ẩn tin COMPLETED/CLOSED (409)")
    void adminJobHideAndSmartRestoreFlow() throws Exception {
        String adminToken = loginAndGetToken("admin@vlu.edu.vn", "Abc@12345");
        String employerToken = loginAndGetToken("hr@cafegocpho.vn", "Abc@12345");

        // 1. Admin xem danh sách tất cả tin tuyển dụng
        mockMvc.perform(get("/api/v1/admin/jobs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(4)));

        // 2. Tin #1 đang OPEN (acceptedCount = 0 < slots = 2): Admin ẩn sang HIDDEN -> 200 OK
        AdminUpdateJobStatusRequest hideReq = AdminUpdateJobStatusRequest.builder().status("HIDDEN").build();
        mockMvc.perform(patch("/api/v1/admin/jobs/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hideReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        // 3. Admin khôi phục tin #1 (acceptedCount = 0 < slots = 2) -> Tự động trở về OPEN
        AdminUpdateJobStatusRequest restoreReq = AdminUpdateJobStatusRequest.builder().status("RESTORE").build();
        mockMvc.perform(patch("/api/v1/admin/jobs/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        // 4. Giả lập tin #3 (slots = 1) có 1 ứng viên ACCEPTED -> chuyển sang FILLED
        String student2Token = loginAndGetToken("sv2@vanlanguni.vn", "Abc@12345");
        MvcResult applyRes = mockMvc.perform(post("/api/v1/jobs/3/applications")
                        .header("Authorization", "Bearer " + student2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverLetter\":\"Em xin ứng tuyển vị trí thiết kế banner.\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long app3Id = objectMapper.readTree(applyRes.getResponse().getContentAsString())
                .path("data").path("applicationId").asLong();

        UpdateApplicationStatusRequest acceptReq = UpdateApplicationStatusRequest.builder().status("ACCEPTED").build();
        mockMvc.perform(patch("/api/v1/applications/" + app3Id + "/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk());

        UpdateJobStatusRequest toFilled = UpdateJobStatusRequest.builder().status("FILLED").build();
        mockMvc.perform(patch("/api/v1/jobs/3/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toFilled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FILLED"));

        // 5. Admin ẩn tin #3 đang FILLED -> HIDDEN, sau đó khôi phục -> Vì acceptedCount (1) == slots (1) nên trở về FILLED
        mockMvc.perform(patch("/api/v1/admin/jobs/3/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hideReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mockMvc.perform(patch("/api/v1/admin/jobs/3/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FILLED"));

        // 6. NTD chuyển tin #3 sang COMPLETED -> Admin thử ẩn tin COMPLETED phải bị chặn 409 INVALID_STATUS_TRANSITION
        UpdateJobStatusRequest toCompleted = UpdateJobStatusRequest.builder().status("COMPLETED").build();
        mockMvc.perform(patch("/api/v1/jobs/3/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toCompleted)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(patch("/api/v1/admin/jobs/3/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hideReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("TC-S5-03 (FR-28, FR-29): Admin CRUD danh mục Ngành nghề & Địa điểm; xóa mục đang có tin/hồ sơ dùng trả về 409 Conflict")
    void adminCatalogCrudAndRestrictDeleteCheck() throws Exception {
        String adminToken = loginAndGetToken("admin@vlu.edu.vn", "Abc@12345");

        // 1. Tạo danh mục ngành nghề mới -> 201 Created
        CategoryRequest createCat = CategoryRequest.builder().name("Trợ lý Nghiên cứu Khoa học").build();
        MvcResult catRes = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Trợ lý Nghiên cứu Khoa học"))
                .andReturn();
        long newCatId = objectMapper.readTree(catRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Tạo trùng tên danh mục -> 409 DUPLICATE_RESOURCE
        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCat)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));

        // 3. Cập nhật tên danh mục mới -> 200 OK
        CategoryRequest updateCat = CategoryRequest.builder().name("Trợ lý Nghiên cứu & Học thuật").build();
        mockMvc.perform(put("/api/v1/admin/categories/" + newCatId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Trợ lý Nghiên cứu & Học thuật"));

        // 4. Xóa danh mục mới chưa có tin dùng -> 200 OK
        mockMvc.perform(delete("/api/v1/admin/categories/" + newCatId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 5. Xóa danh mục #1 đang có tin tuyển dụng tham chiếu -> 409 RESOURCE_IN_USE
        mockMvc.perform(delete("/api/v1/admin/categories/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_IN_USE"));

        // 6. Tạo địa điểm mới -> sửa -> xóa thành công; xóa địa điểm #1 đang có tin/hồ sơ dùng -> 409 RESOURCE_IN_USE
        LocationRequest createLoc = LocationRequest.builder()
                .city("TP. Hồ Chí Minh")
                .district("Huyện Nhà Bè")
                .build();
        MvcResult locRes = mockMvc.perform(post("/api/v1/admin/locations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLoc)))
                .andExpect(status().isCreated())
                .andReturn();
        long newLocId = objectMapper.readTree(locRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/admin/locations/" + newLocId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/locations/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_IN_USE"));
    }

    @Test
    @DisplayName("TC-S5-04 (FR-30, FR-31): Admin ẩn đánh giá không phù hợp (HIDDEN) khiến đánh giá biến mất khỏi hồ sơ công khai và xem thống kê /admin/stats")
    void adminReviewModerationAndSystemStatsFlow() throws Exception {
        String adminToken = loginAndGetToken("admin@vlu.edu.vn", "Abc@12345");
        String employerToken = loginAndGetToken("hr@cafegocpho.vn", "Abc@12345");
        String studentToken = loginAndGetToken("sv1@vanlanguni.vn", "Abc@12345");

        // 1. Hoàn thành công việc #1 và tạo đánh giá từ SV -> NTD
        UpdateApplicationStatusRequest acceptReq = UpdateApplicationStatusRequest.builder().status("ACCEPTED").build();
        mockMvc.perform(patch("/api/v1/applications/1/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/jobs/1/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FILLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/jobs/1/status")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        CreateReviewRequest reviewReq = CreateReviewRequest.builder()
                .revieweeId(2L)
                .rating(5)
                .comment("Đánh giá mẫu cần kiểm duyệt.")
                .build();
        MvcResult revRes = mockMvc.perform(post("/api/v1/jobs/1/reviews")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long reviewId = objectMapper.readTree(revRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Kiểm tra trước khi ẩn: đánh giá reviewId có mặt trong danh sách VISIBLE của user #2
        mockMvc.perform(get("/api/v1/users/2/reviews")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[?(@.id == " + reviewId + ")]").exists());

        // 3. Admin ẩn đánh giá sang HIDDEN (FR-30)
        UpdateReviewStatusRequest hideReviewReq = UpdateReviewStatusRequest.builder().status("HIDDEN").build();
        mockMvc.perform(patch("/api/v1/admin/reviews/" + reviewId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hideReviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        // 4. Kiểm tra GET /api/v1/users/2/reviews chỉ trả về VISIBLE -> không còn reviewId vừa ẩn
        mockMvc.perform(get("/api/v1/users/2/reviews")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[?(@.id == " + reviewId + ")]").isEmpty());

        // 5. Kiểm tra GET /api/v1/admin/stats (FR-31)
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.data.totalStudents", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.totalEmployers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalAdmins").value(1))
                .andExpect(jsonPath("$.data.hiddenReviews", greaterThanOrEqualTo(1)));
    }
}

