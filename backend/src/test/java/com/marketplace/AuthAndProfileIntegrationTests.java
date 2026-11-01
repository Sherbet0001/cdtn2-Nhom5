package com.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.config.JwtProperties;
import com.marketplace.dtos.LoginRequest;
import com.marketplace.dtos.RegisterRequest;
import com.marketplace.dtos.UpdateProfileRequest;
import com.marketplace.entities.User;
import com.marketplace.repositories.CategoryRepository;
import com.marketplace.repositories.LocationRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.UserRepository;
import com.marketplace.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp cho Sprint 1 (Module Auth & Profile): FR-01, FR-02, FR-03, FR-04, FR-05, FR-11, FR-16 và NFR-02.
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class AuthAndProfileIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("SQL Init Idempotency: Chạy lại data.sql nhiều lần liên tiếp (INSERT IGNORE) không gây lỗi trùng khóa")
    void dataSqlInit_idempotentExecution_runsMultipleTimesWithoutError() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }
        assertThat(categoryRepository.count()).isEqualTo(8L);
        assertThat(locationRepository.count()).isEqualTo(10L);
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(6L);
        assertThat(profileRepository.count()).isGreaterThanOrEqualTo(6L);
    }

    @Test
    @DisplayName("FR-01 & NFR-02(a): Đăng ký tài khoản Sinh viên thành công, tạo Profile 1-1 và lưu mật khẩu băm BCrypt trong CSDL")
    void registerUser_FR01_NFR02a_validStudentInput_returnsCreatedUserAndHashesPasswordInDb() throws Exception {
        String rawPassword = "Abc@12345";
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Sinh Viên Mới")
                .email("sinhvienmoi@vanlanguni.vn")
                .password(rawPassword)
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

        // NFR-02(a): Xác nhận mật khẩu lưu trong CSDL là bản băm BCrypt, tuyệt đối không lưu bản thô
        User savedUser = userRepository.findByEmail("sinhvienmoi@vanlanguni.vn").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches(rawPassword, savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("FR-01: Đăng ký thất bại khi email đã tồn tại trong hệ thống (409 CONFLICT - EMAIL_ALREADY_EXISTS)")
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
    @DisplayName("FR-02: Đăng nhập thành công với thông tin hợp lệ, trả về JWT Bearer Token có thời hạn 7200 giây (2 giờ)")
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
    @DisplayName("FR-02: Đăng nhập thất bại khi sai mật khẩu, trả về 401 UNAUTHORIZED (INVALID_CREDENTIALS)")
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
    @DisplayName("FR-03: Đăng xuất tài khoản thành công khi đã xác thực JWT")
    void logoutUser_FR03_authenticatedUser_returnsSuccessResponse() throws Exception {
        String token = obtainToken("khoa.2374802013428@vanlanguni.vn", "Abc@12345");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));
    }

    @Test
    @DisplayName("FR-04 & NFR-02(b): Gọi API bảo vệ không đính kèm JWT Token trả về 401 UNAUTHORIZED")
    void getMyProfile_FR04_NFR02b_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_ACCESS"));
    }

    @Test
    @DisplayName("FR-04 & NFR-02(c): Sinh viên (STUDENT) gọi API của Nhà tuyển dụng (EMPLOYER) hoặc Quản trị viên (ADMIN) trả về 403 FORBIDDEN")
    void accessRestrictedApi_FR04_NFR02c_studentCallingEmployerOrAdminApi_returnsForbidden403() throws Exception {
        String studentToken = obtainToken("khoa.2374802013428@vanlanguni.vn", "Abc@12345");

        // SV gọi API dành riêng cho Nhà tuyển dụng (EMPLOYER) -> 403 FORBIDDEN
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_RESOURCE"));

        // SV gọi API dành riêng cho Quản trị viên (ADMIN) -> 403 FORBIDDEN
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_RESOURCE"));
    }

    @Test
    @DisplayName("FR-04 & NFR-02(d): Gọi API với JWT Token đã hết hạn bị từ chối với 401 UNAUTHORIZED (TOKEN_EXPIRED)")
    void getMyProfile_FR04_NFR02d_expiredToken_returnsUnauthorizedTokenExpired() throws Exception {
        JwtProvider expiredJwtProvider = new JwtProvider(jwtProperties.getSecret(), -60L);
        String expiredToken = expiredJwtProvider.generateToken(4L, "khoa.2374802013428@vanlanguni.vn", "STUDENT");

        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("FR-05: Xem thông tin hồ sơ cá nhân hiện tại và danh mục địa điểm/ngành nghề thành công")
    void getMyProfile_FR05_authenticatedUser_returnsProfileDetails() throws Exception {
        String token = obtainToken("khoa.2374802013428@vanlanguni.vn", "Abc@12345");

        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("khoa.2374802013428@vanlanguni.vn"))
                .andExpect(jsonPath("$.data.fullName").value("Bùi Đăng Khoa"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    @DisplayName("FR-11: Sinh viên cập nhật hồ sơ học vấn và CV trực tuyến thành công, bỏ qua trường doanh nghiệp của NTD")
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
    @DisplayName("FR-16: Nhà tuyển dụng cập nhật thông tin doanh nghiệp thành công, bỏ qua trường CV của Sinh viên")
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
