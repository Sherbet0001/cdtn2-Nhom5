package com.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dtos.LoginRequest;
import com.marketplace.dtos.RegisterRequest;
import com.marketplace.dtos.UpdateProfileRequest;
import com.marketplace.entities.User;
import com.marketplace.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp cho Sprint 1 (Module Auth & Profile): FR-01, FR-02, FR-04, FR-05, FR-11, FR-16.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndProfileIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerUser_FR01_validStudentInput_returnsCreatedUserAndCreatesProfile() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Sinh Viên Mới")
                .email("sinhvienmoi@vanlanguni.vn")
                .password("Abc@12345")
                .role("STUDENT")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("sinhvienmoi@vanlanguni.vn"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void registerUser_FR01_duplicateEmail_throwsEmailAlreadyExists() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Bùi Đăng Khoa Bản Sao")
                .email("khoa.2374802013428@vanlanguni.vn")
                .password("Abc@12345")
                .role("STUDENT")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void loginUser_FR02_validCredentials_returnsJwtTokenWithTwoHoursExpiry() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("khoa.2374802013428@vanlanguni.vn")
                .password("Abc@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.fullName").value("Bùi Đăng Khoa"));
    }

    @Test
    void loginUser_FR02_wrongPassword_returnsUnauthorizedJson() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("khoa.2374802013428@vanlanguni.vn")
                .password("SaiMatKhau123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void getMyProfile_FR04_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
    }

    @Test
    void updateMyProfile_FR11_studentRole_updatesCvAndAcademicInfo() throws Exception {
        String token = obtainToken("khoa.2374802013428@vanlanguni.vn", "Abc@12345");

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .fullName("Bùi Đăng Khoa")
                .phone("0914333999")
                .locationId(5L)
                .bio("Sinh viên năm 3 chuyên ngành CNPM tại ĐH Văn Lang")
                .school("Đại học Văn Lang")
                .major("Kỹ thuật phần mềm")
                .cvContent("Kỹ năng: Spring Security, JWT, ReactJS. Đã hoàn thành Sprint 1.")
                .companyName("Công ty giả mạo không được phép lưu cho SV")
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value("0914333999"))
                .andExpect(jsonPath("$.data.locationId").value(5))
                .andExpect(jsonPath("$.data.major").value("Kỹ thuật phần mềm"))
                .andExpect(jsonPath("$.data.cvContent").value("Kỹ năng: Spring Security, JWT, ReactJS. Đã hoàn thành Sprint 1."))
                .andExpect(jsonPath("$.data.companyName").doesNotExist());
    }

    @Test
    void updateMyProfile_FR16_employerRole_updatesCompanyInfoAndIgnoresStudentFields() throws Exception {
        String token = obtainToken("hr@cafegocpho.vn", "Abc@12345");

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .fullName("Nguyễn Minh Hoàng")
                .phone("0902111888")
                .locationId(3L)
                .bio("Hệ thống quán cà phê acoustic tuyển dụng nhân sự ca tối")
                .companyName("Chuỗi Cafe Góc Phố Sài Gòn")
                .cvContent("Trường CV không áp dụng cho NTD")
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("Chuỗi Cafe Góc Phố Sài Gòn"))
                .andExpect(jsonPath("$.data.phone").value("0902111888"))
                .andExpect(jsonPath("$.data.cvContent").doesNotExist());
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
        String token = root.path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
