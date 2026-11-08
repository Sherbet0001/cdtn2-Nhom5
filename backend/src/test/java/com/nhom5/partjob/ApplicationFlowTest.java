package com.nhom5.partjob;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom5.partjob.dto.AuthDtos.LoginResponse;
import com.nhom5.partjob.dto.AuthDtos.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private String registerAndLogin(String role, String fullName) throws Exception {
        String email = UUID.randomUUID() + "@partjob.test";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(fullName, email,
                                "Abc@12345", role))))
                .andExpect(status().isCreated());
        String response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", "Abc@12345"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readValue(response, LoginResponse.class).accessToken();
    }

    private long createJob(String employerToken, String title) throws Exception {
        String response = mvc.perform(post("/jobs").header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"Mô tả công việc","jobType":"PART_TIME",
                                 "salaryMin":25000,"salaryMax":35000}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }

    private void addStudentCv(String studentToken, String studentName) throws Exception {
        mvc.perform(put("/profiles/me").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "fullName", studentName,
                                "phone", "0901234567",
                                "school", "Đại học Văn Lang",
                                "major", "Công nghệ phần mềm",
                                "bio", "Sinh viên năng động",
                                "cvContent", "Kỹ năng Java, React; kinh nghiệm làm dự án nhóm."
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void studentAppliesEmployerInvitesInterviewAndStudentReceivesNotification() throws Exception {
        String employerToken = registerAndLogin("EMPLOYER", "Cửa hàng Cà phê");
        String otherEmployerToken = registerAndLogin("EMPLOYER", "Nhà tuyển dụng khác");
        String studentToken = registerAndLogin("STUDENT", "Nguyễn An");
        String jobTitle = "Barista " + UUID.randomUUID();
        long jobId = createJob(employerToken, jobTitle);
        addStudentCv(studentToken, "Nguyễn An");

        String applied = mvc.perform(post("/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverLetter\":\"Em có thể làm ca tối và cuối tuần.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.cvContent").value("Kỹ năng Java, React; kinh nghiệm làm dự án nhóm."))
                .andReturn().getResponse().getContentAsString();
        long applicationId = mapper.readTree(applied).get("id").asLong();

        mvc.perform(get("/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + otherEmployerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mvc.perform(patch("/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + otherEmployerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].student.fullName").value("Nguyễn An"))
                .andExpect(jsonPath("$[0].student.email").exists())
                .andExpect(jsonPath("$[0].cvContent").exists())
                .andExpect(jsonPath("$[0].coverLetter").value("Em có thể làm ca tối và cuối tuần."));

        mvc.perform(get("/applications/employer/mine")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(jobId))
                .andExpect(jsonPath("$[0].student.fullName").value("Nguyễn An"));

        mvc.perform(get("/applications/employer/mine")
                        .header("Authorization", "Bearer " + otherEmployerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        String interviewAt = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        mvc.perform(patch("/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "status", "INTERVIEW",
                                "interviewAt", interviewAt,
                                "interviewLocation", "Cửa hàng Quận 1",
                                "employerMessage", "Vui lòng mang theo CCCD."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));

        mvc.perform(get("/applications/mine").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INTERVIEW"))
                .andExpect(jsonPath("$[0].interviewLocation").value("Cửa hàng Quận 1"));

        String notifications = mvc.perform(get("/notifications/mine")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].status").value("INTERVIEW"))
                .andExpect(jsonPath("$.items[0].jobTitle").value(jobTitle))
                .andReturn().getResponse().getContentAsString();
        JsonNode notification = mapper.readTree(notifications).get("items").get(0);
        long notificationId = notification.get("id").asLong();

        mvc.perform(patch("/notifications/{id}/read", notificationId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mvc.perform(get("/notifications/mine").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void employerCanRejectAndDuplicateApplicationsAreRejected() throws Exception {
        String employerToken = registerAndLogin("EMPLOYER", "Nhà tuyển dụng");
        String studentToken = registerAndLogin("STUDENT", "Trần Bình");
        long jobId = createJob(employerToken, "Trợ lý cửa hàng " + UUID.randomUUID());
        addStudentCv(studentToken, "Trần Bình");

        String applied = mvc.perform(post("/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long applicationId = mapper.readTree(applied).get("id").asLong();

        mvc.perform(post("/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());

        mvc.perform(patch("/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"employerMessage\":\"Đã tuyển đủ nhân sự.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mvc.perform(get("/notifications/mine").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("REJECTED"));
    }

    @Test
    void studentCanUploadCvFileAndEmployerCanDownloadApplicationSnapshot() throws Exception {
        String employerToken = registerAndLogin("EMPLOYER", "Nhà tuyển dụng");
        String otherEmployerToken = registerAndLogin("EMPLOYER", "Nhà tuyển dụng khác");
        String studentToken = registerAndLogin("STUDENT", "Sinh viên có CV tệp");
        long jobId = createJob(employerToken, "Trợ lý cửa hàng " + UUID.randomUUID());
        byte[] cvBytes = "%PDF-1.7\nCV sample".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile cvFile = new MockMultipartFile(
                "file", "cv-sinh-vien.pdf", "application/pdf", cvBytes);
        MockHttpServletRequestBuilder upload = multipart("/profiles/me/cv-file")
                .file(cvFile)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });

        mvc.perform(upload.header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvFileName").value("cv-sinh-vien.pdf"));

        String application = mvc.perform(post("/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cvContent").value(nullValue()))
                .andExpect(jsonPath("$.cvFileName").value("cv-sinh-vien.pdf"))
                .andReturn().getResponse().getContentAsString();
        long applicationId = mapper.readTree(application).get("id").asLong();

        byte[] replacementBytes = "%PDF-1.7\nUpdated CV".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile replacementFile = new MockMultipartFile(
                "file", "cv-cap-nhat.pdf", "application/pdf", replacementBytes);
        mvc.perform(multipart("/profiles/me/cv-file")
                        .file(replacementFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvFileName").value("cv-cap-nhat.pdf"));

        mvc.perform(get("/profiles/me/cv-file").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(replacementBytes));

        mvc.perform(get("/applications/{applicationId}/cv-file", applicationId)
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(cvBytes));

        mvc.perform(get("/applications/{applicationId}/cv-file", applicationId)
                        .header("Authorization", "Bearer " + otherEmployerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void cvUploadRejectsUnsupportedFormatsAndInvalidFileContents() throws Exception {
        String studentToken = registerAndLogin("STUDENT", "Sinh viên");
        MockMultipartFile unsupported = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "not a CV".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/profiles/me/cv-file")
                        .file(unsupported)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ chấp nhận tệp PDF, DOC hoặc DOCX"));

        MockMultipartFile invalidPdf = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "not a PDF".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/profiles/me/cv-file")
                        .file(invalidPdf)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nội dung tệp không khớp định dạng PDF, DOC hoặc DOCX"));
    }

    @Test
    void applicationEndpointsEnforceRolesAndCvRequirement() throws Exception {
        String employerToken = registerAndLogin("EMPLOYER", "Nhà tuyển dụng");
        String studentWithoutCv = registerAndLogin("STUDENT", "Sinh viên mới");
        long jobId = createJob(employerToken, "Nhân viên bán hàng " + UUID.randomUUID());

        mvc.perform(post("/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + studentWithoutCv)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng nhập nội dung CV hoặc tải tệp CV trong hồ sơ trước khi ứng tuyển"));

        mvc.perform(multipart("/profiles/me/cv-file")
                        .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8)))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isForbidden());

        mvc.perform(get("/applications/mine").header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isForbidden());

        mvc.perform(get("/applications/employer/mine").header("Authorization", "Bearer " + studentWithoutCv))
                .andExpect(status().isForbidden());

        mvc.perform(get("/notifications/mine").header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isForbidden());
    }
}
