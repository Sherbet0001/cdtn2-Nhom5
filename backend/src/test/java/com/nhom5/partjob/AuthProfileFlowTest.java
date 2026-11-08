package com.nhom5.partjob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom5.partjob.domain.User;
import com.nhom5.partjob.domain.UserStatus;
import com.nhom5.partjob.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiem thu luong uu tien cao cua Sprint buoi 6: dang ky -> dang nhap -> xem/sua ho so -> dang xuat,
 * cung cac tinh huong loi 400/401/403/409 theo API contract. Chay tren H2 (profile "test").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthProfileFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    UserRepository userRepository;

    private String registerJson(String name, String email, String password, String role) {
        return """
                {"fullName":"%s","email":"%s","password":"%s","role":"%s"}
                """.formatted(name, email, password, role);
    }

    private void register(String name, String email, String password, String role) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(name, email, password, role)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void studentFlow_register_login_profile_cv_logout() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Bùi Đăng Khoa", "khoa@student.vlu.edu.vn", "Abc@12345", "STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.userId").isNumber());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"khoa@student.vlu.edu.vn\",\"password\":\"Abc@12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(7200))
                .andExpect(jsonPath("$.user.fullName").value("Bùi Đăng Khoa"));

        String token = loginAndGetToken("khoa@student.vlu.edu.vn", "Abc@12345");

        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Bùi Đăng Khoa"))
                .andExpect(jsonPath("$.cvContent").doesNotExist());

        String update = """
                {"fullName":"Bùi Đăng Khoa","phone":"0901 234 567","locationId":1,
                 "bio":"Sinh viên năm 3, muốn làm thêm cuối tuần","school":"ĐH Văn Lang","major":"Công nghệ phần mềm",
                 "cvContent":"Kỹ năng: giao tiếp, Excel. Kinh nghiệm: phục vụ quán cà phê 6 tháng.",
                 "companyName":"Trường này không áp dụng cho sinh viên"}
                """;
        mvc.perform(put("/profiles/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.school").value("ĐH Văn Lang"))
                .andExpect(jsonPath("$.cvContent", containsString("Excel")))
                .andExpect(jsonPath("$.companyName", nullValue()))
                .andExpect(jsonPath("$.location.id").value(1));

        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.major").value("Công nghệ phần mềm"));

        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void employerProfile_onlyCompanyNameIsStored() throws Exception {
        register("Cafe Góc Phố", "ntd@cafe.vn", "Abc@12345", "EMPLOYER");
        String token = loginAndGetToken("ntd@cafe.vn", "Abc@12345");

        String update = """
                {"fullName":"Nguyễn Văn A","companyName":"Cafe Góc Phố","bio":"Quán cà phê quận 7",
                 "school":"khong-luu","cvContent":"khong-luu"}
                """;
        mvc.perform(put("/profiles/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Cafe Góc Phố"))
                .andExpect(jsonPath("$.school", nullValue()))
                .andExpect(jsonPath("$.cvContent", nullValue()));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register("An", "trung@mail.com", "Abc@12345", "STUDENT");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("An 2", "TRUNG@mail.com", "Abc@12345", "EMPLOYER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void register_invalidData_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Binh", "binh@mail.com", "123", "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].message").value("Mật khẩu tối thiểu 8 ký tự"));
    }

    @Test
    void register_adminRole_isRejected() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Hacker", "hacker@mail.com", "Abc@12345", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        register("Chi", "chi@mail.com", "Abc@12345", "STUDENT");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"chi@mail.com\",\"password\":\"sai-mat-khau\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/profiles/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mvc.perform(get("/profiles/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rbac_studentCannotAccessAdminArea_returns403() throws Exception {
        register("Dung", "dung@mail.com", "Abc@12345", "STUDENT");
        String token = loginAndGetToken("dung@mail.com", "Abc@12345");
        mvc.perform(get("/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void seededAdmin_canLogin_andHasAdminRole() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@partjob.local\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void lockedAccount_cannotLoginOrUseAnExistingToken() throws Exception {
        register("Mai", "mai@student.vlu.edu.vn", "Abc@12345", "STUDENT");
        String token = loginAndGetToken("mai@student.vlu.edu.vn", "Abc@12345");

        User user = userRepository.findByEmail("mai@student.vlu.edu.vn").orElseThrow();
        user.setStatus(UserStatus.LOCKED);
        userRepository.saveAndFlush(user);

        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mai@student.vlu.edu.vn\",\"password\":\"Abc@12345\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void locations_arePublicAndAvailableForProfileForm() throws Exception {
        mvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].city").isNotEmpty())
                .andExpect(jsonPath("$[0].district").isNotEmpty());
    }
}
