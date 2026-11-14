package com.vlu.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlu.freelance.dto.JobCreateRequest;
import com.vlu.freelance.dto.JobResponse;
import com.vlu.freelance.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @Test
    @DisplayName("TC-17 [Integration/FR-07]: GET /api/v1/jobs -> Trả về danh sách phân trang HTTP 200")
    void testGetJobs_ReturnsHttp200() throws Exception {
        JobResponse response = new JobResponse();
        response.setId(1L);
        response.setTitle("Phục vụ quán cà phê");
        response.setStatus("OPEN");

        when(jobService.getJobs(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/jobs")
                        .param("keyword", "phục vụ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Phục vụ quán cà phê"));
    }

    @Test
    @DisplayName("TC-18 [Integration/FR-17]: POST /api/v1/jobs -> Đăng tin hợp lệ trả về HTTP 201")
    void testCreateJob_ReturnsHttp201() throws Exception {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Gia sư dạy kèm");
        request.setDescription("Dạy kèm toán lớp 9");
        request.setCategoryId(3L);
        request.setLocationId(5L);
        request.setSalaryMin(BigDecimal.valueOf(150000));
        request.setSalaryMax(BigDecimal.valueOf(200000));
        request.setSalaryUnit("HOUR");
        request.setWorkSchedule("Tối thứ 3, 5");
        request.setStartDate(LocalDate.of(2026, 11, 20));
        request.setEndDate(LocalDate.of(2026, 12, 31));
        request.setSlots(1);

        when(jobService.createJob(any(JobCreateRequest.class), anyLong()))
                .thenReturn(Map.of("jobId", 15L, "status", "OPEN"));

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(15L))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }
}
