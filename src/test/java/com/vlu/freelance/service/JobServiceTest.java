package com.vlu.freelance.service;

import com.vlu.freelance.dto.JobCreateRequest;
import com.vlu.freelance.dto.JobResponse;
import com.vlu.freelance.dto.JobUpdateRequest;
import com.vlu.freelance.entity.Job;
import com.vlu.freelance.exception.BusinessRuleException;
import com.vlu.freelance.exception.ForbiddenException;
import com.vlu.freelance.exception.ResourceNotFoundException;
import com.vlu.freelance.repository.CategoryRepository;
import com.vlu.freelance.repository.JobRepository;
import com.vlu.freelance.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private JobService jobService;

    private Job sampleJob;
    private JobCreateRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        sampleJob = new Job();
        sampleJob.setId(1L);
        sampleJob.setEmployerId(2L);
        sampleJob.setCategoryId(1L);
        sampleJob.setLocationId(3L);
        sampleJob.setTitle("Nhân viên phục vụ quán cà phê");
        sampleJob.setDescription("Phục vụ bàn ca tối cuối tuần");
        sampleJob.setSalaryMin(BigDecimal.valueOf(25000));
        sampleJob.setSalaryMax(BigDecimal.valueOf(30000));
        sampleJob.setSalaryUnit("HOUR");
        sampleJob.setWorkSchedule("Thứ 7, CN (18:00 - 22:30)");
        sampleJob.setStartDate(LocalDate.of(2026, 11, 15));
        sampleJob.setEndDate(LocalDate.of(2026, 12, 31));
        sampleJob.setSlots(3);
        sampleJob.setStatus("OPEN");

        validCreateRequest = new JobCreateRequest();
        validCreateRequest.setTitle("Nhân viên thu ngân");
        validCreateRequest.setDescription("Tính tiền hóa đơn");
        validCreateRequest.setCategoryId(2L);
        validCreateRequest.setLocationId(1L);
        validCreateRequest.setSalaryMin(BigDecimal.valueOf(28000));
        validCreateRequest.setSalaryMax(BigDecimal.valueOf(32000));
        validCreateRequest.setSalaryUnit("HOUR");
        validCreateRequest.setWorkSchedule("Ca sáng 08:00 - 12:00");
        validCreateRequest.setStartDate(LocalDate.of(2026, 11, 20));
        validCreateRequest.setEndDate(LocalDate.of(2026, 12, 31));
        validCreateRequest.setSlots(2);
    }

    @Test
    @DisplayName("TC-01 [FR-17]: Đăng tin với dữ liệu hợp lệ -> Thành công (OPEN)")
    void testCreateJob_Success() {
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Map<String, Object> result = jobService.createJob(validCreateRequest, 2L);

        assertNotNull(result);
        assertEquals(10L, result.get("jobId"));
        assertEquals("OPEN", result.get("status"));
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    @DisplayName("TC-02 [FR-17]: Đăng tin với salaryMin > salaryMax -> Báo lỗi IllegalArgumentException")
    void testCreateJob_InvalidSalary_ThrowsException() {
        validCreateRequest.setSalaryMin(BigDecimal.valueOf(40000));
        validCreateRequest.setSalaryMax(BigDecimal.valueOf(30000)); // Lỗi: min > max

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                jobService.createJob(validCreateRequest, 2L)
        );

        assertTrue(ex.getMessage().contains("Lương tối thiểu không được lớn hơn lương tối đa"));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("TC-03 [FR-17]: Đăng tin với startDate sau endDate -> Báo lỗi IllegalArgumentException")
    void testCreateJob_InvalidDate_ThrowsException() {
        validCreateRequest.setStartDate(LocalDate.of(2026, 12, 31));
        validCreateRequest.setEndDate(LocalDate.of(2026, 11, 1)); // Lỗi: start > end

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                jobService.createJob(validCreateRequest, 2L)
        );

        assertTrue(ex.getMessage().contains("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc"));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("TC-04 [FR-10]: Xem chi tiết tin tồn tại -> Trả về JobResponse thành công")
    void testGetJobById_Found() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        JobResponse response = jobService.getJobById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Nhân viên phục vụ quán cà phê", response.getTitle());
    }

    @Test
    @DisplayName("TC-05 [FR-10]: Xem chi tiết tin không tồn tại -> Ném ResourceNotFoundException (404)")
    void testGetJobById_NotFound_ThrowsException() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.getJobById(999L));
    }

    @Test
    @DisplayName("TC-06 [FR-18]: Sửa tin đã COMPLETED -> Chặn và ném BusinessRuleException (409)")
    void testUpdateJob_WhenCompleted_ThrowsException() {
        sampleJob.setStatus("COMPLETED");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        JobUpdateRequest updateRequest = new JobUpdateRequest();
        updateRequest.setTitle("Đổi tiêu đề");
        updateRequest.setDescription("Mô tả mới");
        updateRequest.setCategoryId(1L);
        updateRequest.setLocationId(3L);
        updateRequest.setSalaryMin(BigDecimal.valueOf(25000));
        updateRequest.setSalaryMax(BigDecimal.valueOf(30000));
        updateRequest.setSalaryUnit("HOUR");
        updateRequest.setWorkSchedule("Lịch mới");
        updateRequest.setStartDate(LocalDate.of(2026, 11, 15));
        updateRequest.setEndDate(LocalDate.of(2026, 12, 31));
        updateRequest.setSlots(2);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                jobService.updateJob(1L, updateRequest, 2L)
        );

        assertTrue(ex.getMessage().contains("COMPLETED"));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("TC-07 [FR-18]: NTD khác sửa tin không thuộc về mình -> Ném ForbiddenException (403)")
    void testUpdateJob_WrongEmployer_ThrowsException() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        JobUpdateRequest updateRequest = new JobUpdateRequest();
        updateRequest.setSalaryMin(BigDecimal.valueOf(25000));
        updateRequest.setSalaryMax(BigDecimal.valueOf(30000));
        updateRequest.setStartDate(LocalDate.of(2026, 11, 15));
        updateRequest.setEndDate(LocalDate.of(2026, 12, 31));

        assertThrows(ForbiddenException.class, () ->
                jobService.updateJob(1L, updateRequest, 99L) // 99L != employerId (2L)
        );
    }

    @Test
    @DisplayName("TC-08 [FR-23]: Máy trạng thái tin: Chuyển OPEN -> FILLED -> COMPLETED -> Thành công")
    void testUpdateJobStatus_ValidTransitions() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        // 1. OPEN -> FILLED
        Map<String, Object> res1 = jobService.updateJobStatus(1L, "FILLED", 2L);
        assertEquals("FILLED", res1.get("status"));

        // 2. FILLED -> COMPLETED
        sampleJob.setStatus("FILLED");
        Map<String, Object> res2 = jobService.updateJobStatus(1L, "COMPLETED", 2L);
        assertEquals("COMPLETED", res2.get("status"));
    }

    @Test
    @DisplayName("TC-09 [FR-23]: Cấm quay ngược trạng thái từ COMPLETED -> Ném BusinessRuleException (409)")
    void testUpdateJobStatus_RevertFromCompleted_ThrowsException() {
        sampleJob.setStatus("COMPLETED");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                jobService.updateJobStatus(1L, "OPEN", 2L)
        );

        assertTrue(ex.getMessage().contains("không thể thay đổi trạng thái nữa"));
        verify(jobRepository, never()).save(any(Job.class));
    }
}
