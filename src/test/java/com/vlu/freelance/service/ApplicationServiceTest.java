package com.vlu.freelance.service;

import com.vlu.freelance.entity.Application;
import com.vlu.freelance.entity.Job;
import com.vlu.freelance.exception.BusinessRuleException;
import com.vlu.freelance.exception.ForbiddenException;
import com.vlu.freelance.repository.ApplicationRepository;
import com.vlu.freelance.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private Job openJob;
    private Job closedJob;
    private Application pendingApplication;

    @BeforeEach
    void setUp() {
        openJob = new Job();
        openJob.setId(1L);
        openJob.setEmployerId(2L);
        openJob.setTitle("Phục vụ quán cà phê");
        openJob.setStatus("OPEN");
        openJob.setSlots(2);

        closedJob = new Job();
        closedJob.setId(2L);
        closedJob.setEmployerId(2L);
        closedJob.setTitle("Thu ngân");
        closedJob.setStatus("CLOSED"); // Tin đã đóng
        closedJob.setSlots(1);

        pendingApplication = new Application();
        pendingApplication.setId(10L);
        pendingApplication.setJobId(1L);
        pendingApplication.setStudentId(5L);
        pendingApplication.setCoverLetter("Em rảnh cuối tuần");
        pendingApplication.setStatus("PENDING");
    }

    @Test
    @DisplayName("TC-10 [FR-12]: Sinh viên nộp hồ sơ vào tin OPEN -> Thành công (status PENDING)")
    void testApplyJob_Success() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.findByJobIdAndStudentId(1L, 5L)).thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> {
            Application app = i.getArgument(0);
            app.setId(100L);
            return app;
        });

        Map<String, Object> result = applicationService.applyJob(1L, 5L, "Em rảnh cuối tuần");

        assertNotNull(result);
        assertEquals(100L, result.get("applicationId"));
        assertEquals("PENDING", result.get("status"));
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    @DisplayName("TC-11 [Integration/FR-12 Slide 5]: Ứng tuyển vào tin đã đóng (CLOSED) -> Hệ thống từ chối đúng (409)")
    void testApplyJob_WhenJobClosed_ThrowsBusinessRuleException() {
        when(jobRepository.findById(2L)).thenReturn(Optional.of(closedJob));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                applicationService.applyJob(2L, 5L, "Lời nhắn ứng tuyển")
        );

        assertTrue(ex.getMessage().contains("không mở nhận hồ sơ"));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("TC-12 [FR-12]: Ứng tuyển lặp lại vào cùng một tin -> Chặn và ném BusinessRuleException (409)")
    void testApplyJob_AlreadyApplied_ThrowsBusinessRuleException() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.findByJobIdAndStudentId(1L, 5L)).thenReturn(Optional.of(pendingApplication));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                applicationService.applyJob(1L, 5L, "Lời nhắn ứng tuyển")
        );

        assertTrue(ex.getMessage().contains("đã ứng tuyển"));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("TC-13 [FR-21]: NTD chấp nhận hồ sơ hợp lệ -> Chuyển sang ACCEPTED")
    void testUpdateApplicationStatus_Accepted_Success() {
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(pendingApplication));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.countByJobIdAndStatus(1L, "ACCEPTED")).thenReturn(0L); // 0 < slots (2)
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = applicationService.updateApplicationStatus(10L, "ACCEPTED", 2L);

        assertEquals("ACCEPTED", result.get("status"));
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    @DisplayName("TC-14 [FR-21]: Duyệt hồ sơ khi đã đủ số lượng slots tuyển -> Chặn và báo lỗi 409")
    void testUpdateApplicationStatus_ExceedSlots_ThrowsBusinessRuleException() {
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(pendingApplication));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.countByJobIdAndStatus(1L, "ACCEPTED")).thenReturn(2L); // 2 == slots (2) -> Đã đủ

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                applicationService.updateApplicationStatus(10L, "ACCEPTED", 2L)
        );

        assertTrue(ex.getMessage().contains("đã nhận đủ số lượng sinh viên"));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("TC-15 [FR-21, 22]: Đổi lại quyết định hồ sơ đã được duyệt/từ chối -> Chặn đổi trạng thái")
    void testUpdateApplicationStatus_AlreadyProcessed_ThrowsException() {
        pendingApplication.setStatus("ACCEPTED"); // Đã duyệt trước đó
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(pendingApplication));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(openJob));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                applicationService.updateApplicationStatus(10L, "REJECTED", 2L)
        );

        assertTrue(ex.getMessage().contains("không thể thay đổi lại"));
    }

    @Test
    @DisplayName("TC-16 [FR-21, 22]: Người dùng không phải chủ tin duyệt hồ sơ -> Báo lỗi 403 Forbidden")
    void testUpdateApplicationStatus_Unauthorized_ThrowsForbiddenException() {
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(pendingApplication));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(openJob));

        assertThrows(ForbiddenException.class, () ->
                applicationService.updateApplicationStatus(10L, "ACCEPTED", 999L) // 999L != employerId (2L)
        );
    }
}
