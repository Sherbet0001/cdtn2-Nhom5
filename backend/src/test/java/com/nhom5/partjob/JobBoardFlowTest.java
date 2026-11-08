package com.nhom5.partjob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom5.partjob.dto.AuthDtos.LoginResponse;
import com.nhom5.partjob.dto.AuthDtos.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobBoardFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private String registerAndLogin(String role) throws Exception {
        String email = UUID.randomUUID() + "@partjob.test";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("Nhà tuyển dụng", email,
                                "Abc@12345", role))))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "email", email, "password", "Abc@12345"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readValue(body, LoginResponse.class).accessToken();
    }

    private String jobJson(String title) {
        return """
                {"title":"%s","description":"Mô tả công việc đầy đủ","requirements":"Có trách nhiệm",
                 "benefits":"Lịch làm linh hoạt","jobType":"PART_TIME","salaryMin":25000,
                 "salaryMax":40000,"locationId":1}
                """.formatted(title);
    }

    @Test
    void employerCanPublishSearchCloseAndReopenOwnJob() throws Exception {
        String employerToken = registerAndLogin("EMPLOYER");
        String title = "Part-time barista " + UUID.randomUUID();

        String created = mvc.perform(post("/jobs").header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jobJson(title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.companyName").value("Nhà tuyển dụng"))
                .andExpect(jsonPath("$.location.id").value(1))
                .andReturn().getResponse().getContentAsString();
        long jobId = mapper.readTree(created).get("id").asLong();

        mvc.perform(get("/jobs").param("q", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(jobId));

        mvc.perform(get("/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Mô tả công việc đầy đủ"));

        mvc.perform(patch("/jobs/{jobId}/status", jobId)
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mvc.perform(get("/jobs/{jobId}", jobId)).andExpect(status().isNotFound());

        mvc.perform(patch("/jobs/{jobId}/status", jobId)
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void employersCannotEditAnotherEmployersJob_andStudentsCannotPost() throws Exception {
        String ownerToken = registerAndLogin("EMPLOYER");
        String otherEmployerToken = registerAndLogin("EMPLOYER");
        String studentToken = registerAndLogin("STUDENT");

        String created = mvc.perform(post("/jobs").header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jobJson("Protected listing")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long jobId = mapper.readTree(created).get("id").asLong();

        mvc.perform(put("/jobs/{jobId}", jobId).header("Authorization", "Bearer " + otherEmployerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jobJson("Unauthorized edit")))
                .andExpect(status().isNotFound());

        mvc.perform(post("/jobs").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jobJson("Student listing")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidSalaryRangeIsRejected() throws Exception {
        String employerToken = registerAndLogin("EMPLOYER");
        mvc.perform(post("/jobs").header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Salary validation","description":"Test",
                                 "jobType":"PART_TIME","salaryMin":50000,"salaryMax":10000}
                                """))
                .andExpect(status().isBadRequest());
    }
}
