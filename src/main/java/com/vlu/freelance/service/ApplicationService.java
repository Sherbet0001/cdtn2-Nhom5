package com.vlu.freelance.service;

import com.vlu.freelance.dto.ApplicationResponse;
import com.vlu.freelance.entity.Application;
import com.vlu.freelance.entity.Job;
import com.vlu.freelance.exception.BusinessRuleException;
import com.vlu.freelance.exception.ForbiddenException;
import com.vlu.freelance.exception.ResourceNotFoundException;
import com.vlu.freelance.repository.ApplicationRepository;
import com.vlu.freelance.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    /**
     * FR-12: Sinh viên ứng tuyển công việc
     */
    @Transactional
    public Map<String, Object> applyJob(Long jobId, Long studentId, String coverLetter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + jobId));

        // Ràng buộc mục 3.4: Tin phải ở trạng thái OPEN
        if (!"OPEN".equalsIgnoreCase(job.getStatus())) {
            throw new BusinessRuleException("Tin tuyển dụng hiện không mở nhận hồ sơ ứng tuyển (Trạng thái: " + job.getStatus() + ")");
        }

        // Ràng buộc mục 3.4: Mỗi sinh viên ứng tuyển một tin tối đa một lần
        if (applicationRepository.findByJobIdAndStudentId(jobId, studentId).isPresent()) {
            throw new BusinessRuleException("Bạn đã ứng tuyển công việc này rồi");
        }

        Application app = new Application();
        app.setJobId(jobId);
        app.setStudentId(studentId);
        app.setCoverLetter(coverLetter);
        app.setStatus("PENDING");

        Application savedApp = applicationRepository.save(app);

        Map<String, Object> response = new HashMap<>();
        response.put("applicationId", savedApp.getId());
        response.put("jobId", savedApp.getJobId());
        response.put("status", savedApp.getStatus());
        response.put("appliedAt", savedApp.getAppliedAt());
        return response;
    }

    /**
     * FR-13, FR-14: Sinh viên xem danh sách hồ sơ ứng tuyển của mình (lịch sử ứng tuyển)
     */
    public Page<ApplicationResponse> getMyApplications(Long studentId, Pageable pageable) {
        Page<Application> apps = applicationRepository.findByStudentId(studentId, pageable);
        return apps.map(app -> {
            ApplicationResponse res = ApplicationResponse.fromEntity(app);
            jobRepository.findById(app.getJobId()).ifPresent(j -> res.setJobTitle(j.getTitle()));
            return res;
        });
    }

    /**
     * FR-13: Chi tiết một hồ sơ ứng tuyển
     */
    public ApplicationResponse getApplicationById(Long id, Long currentUserId) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển với ID: " + id));

        Job job = jobRepository.findById(app.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng của hồ sơ này"));

        // Chỉ SV chủ hồ sơ hoặc NTD chủ tin mới được xem
        if (!app.getStudentId().equals(currentUserId) && !job.getEmployerId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền xem thông tin hồ sơ ứng tuyển này");
        }

        ApplicationResponse res = ApplicationResponse.fromEntity(app);
        res.setJobTitle(job.getTitle());
        return res;
    }

    /**
     * FR-19: Nhà tuyển dụng xem danh sách sinh viên ứng tuyển vào tin của mình
     */
    public Page<ApplicationResponse> getJobApplications(Long jobId, Long employerId, Pageable pageable) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + jobId));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách ứng viên của tin này");
        }

        Page<Application> apps = applicationRepository.findByJobId(jobId, pageable);
        return apps.map(app -> {
            ApplicationResponse res = ApplicationResponse.fromEntity(app);
            res.setJobTitle(job.getTitle());
            return res;
        });
    }

    /**
     * FR-21, FR-22: Nhà tuyển dụng duyệt (ACCEPTED) hoặc từ chối (REJECTED) hồ sơ
     */
    @Transactional
    public Map<String, Object> updateApplicationStatus(Long id, String newStatus, Long employerId) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển với ID: " + id));

        Job job = jobRepository.findById(app.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng của hồ sơ này"));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ForbiddenException("Bạn không có quyền duyệt hồ sơ ứng tuyển này");
        }

        // Quy tắc mục 3.4: PENDING -> ACCEPTED hoặc REJECTED, quyết định cuối cùng, không đổi lại
        if (!"PENDING".equalsIgnoreCase(app.getStatus())) {
            throw new BusinessRuleException("Hồ sơ đã được xử lý (" + app.getStatus() + "), không thể thay đổi lại");
        }

        // Quy tắc mục 3.4: số hồ sơ ACCEPTED của một tin không vượt quá slots
        if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
            long currentAccepted = applicationRepository.countByJobIdAndStatus(job.getId(), "ACCEPTED");
            if (currentAccepted >= job.getSlots()) {
                throw new BusinessRuleException("Tin tuyển dụng đã nhận đủ số lượng sinh viên (" + job.getSlots() + " slots)");
            }
        }

        app.setStatus(newStatus.toUpperCase());
        Application saved = applicationRepository.save(app);

        Map<String, Object> response = new HashMap<>();
        response.put("applicationId", saved.getId());
        response.put("status", saved.getStatus());
        response.put("updatedAt", saved.getUpdatedAt());
        return response;
    }
}
