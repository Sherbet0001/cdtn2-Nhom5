package com.vlu.freelance.service;

import com.vlu.freelance.dto.JobCreateRequest;
import com.vlu.freelance.dto.JobResponse;
import com.vlu.freelance.dto.JobUpdateRequest;
import com.vlu.freelance.entity.Category;
import com.vlu.freelance.entity.Job;
import com.vlu.freelance.entity.Location;
import com.vlu.freelance.exception.BusinessRuleException;
import com.vlu.freelance.exception.ForbiddenException;
import com.vlu.freelance.exception.ResourceNotFoundException;
import com.vlu.freelance.repository.CategoryRepository;
import com.vlu.freelance.repository.JobRepository;
import com.vlu.freelance.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    /**
     * Chuyển đổi Entity Job sang JobResponse DTO kèm tên ngành nghề và địa điểm
     */
    public JobResponse mapToJobResponse(Job job) {
        JobResponse response = JobResponse.fromEntity(job);

        // Nạp tên ngành nghề nếu có
        if (job.getCategoryId() != null) {
            categoryRepository.findById(job.getCategoryId())
                    .ifPresent(cat -> response.setCategory(cat.getName()));
        }

        // Nạp định dạng địa điểm nếu có
        if (job.getLocationId() != null) {
            locationRepository.findById(job.getLocationId())
                    .ifPresent(loc -> response.setLocation(loc.getFormattedLocation()));
        }

        return response;
    }

    /**
     * FR-17: Đăng tin tuyển dụng mới (Vai trò: Nhà tuyển dụng)
     */
    @Transactional
    public Map<String, Object> createJob(JobCreateRequest request, Long employerId) {
        if (request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new IllegalArgumentException("Lương tối thiểu không được lớn hơn lương tối đa");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        Job job = new Job();
        job.setEmployerId(employerId);
        job.setCategoryId(request.getCategoryId());
        job.setLocationId(request.getLocationId());
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSalaryUnit(request.getSalaryUnit().toUpperCase());
        job.setWorkSchedule(request.getWorkSchedule());
        job.setStartDate(request.getStartDate());
        job.setEndDate(request.getEndDate());
        job.setSlots(request.getSlots());
        job.setStatus("OPEN"); // Mặc định OPEN theo FR-17

        Job savedJob = jobRepository.save(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", savedJob.getId());
        response.put("status", savedJob.getStatus());
        return response;
    }

    /**
     * FR-07, FR-08, FR-09: Tìm kiếm & lọc danh sách việc làm part-time
     */
    public Page<JobResponse> getJobs(String keyword, Long categoryId, Long locationId, BigDecimal minSalary, String salaryUnit, Pageable pageable) {
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String unit = (salaryUnit != null && !salaryUnit.trim().isEmpty()) ? salaryUnit.trim().toUpperCase() : null;
        // Quy tắc Buổi 5: lọc lương tối thiểu theo cùng đơn vị HOUR, DAY, MONTH hoặc TASK; thiếu salaryUnit thì bỏ qua minSalary
        BigDecimal validMinSalary = (unit != null) ? minSalary : null;

        Page<Job> jobs = jobRepository.searchJobs(searchKeyword, categoryId, locationId, validMinSalary, unit, pageable);
        return jobs.map(this::mapToJobResponse);
    }

    /**
     * FR-10: Xem chi tiết thông tin một việc làm
     */
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + id));
        return mapToJobResponse(job);
    }

    /**
     * FR-18: Xem danh sách tin tuyển dụng của NTD (mọi trạng thái)
     */
    public Page<JobResponse> getMyJobs(Long employerId, Pageable pageable) {
        Page<Job> jobs = jobRepository.findByEmployerId(employerId, pageable);
        return jobs.map(this::mapToJobResponse);
    }

    /**
     * FR-18: Chỉnh sửa nội dung tin tuyển dụng (khi chưa COMPLETED)
     */
    @Transactional
    public JobResponse updateJob(Long id, JobUpdateRequest request, Long employerId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + id));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa tin tuyển dụng này");
        }

        if ("COMPLETED".equalsIgnoreCase(job.getStatus())) {
            throw new BusinessRuleException("Không thể chỉnh sửa tin tuyển dụng đã ở trạng thái hoàn thành (COMPLETED)");
        }

        if (request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new IllegalArgumentException("Lương tối thiểu không được lớn hơn lương tối đa");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        job.setCategoryId(request.getCategoryId());
        job.setLocationId(request.getLocationId());
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSalaryUnit(request.getSalaryUnit().toUpperCase());
        job.setWorkSchedule(request.getWorkSchedule());
        job.setStartDate(request.getStartDate());
        job.setEndDate(request.getEndDate());
        job.setSlots(request.getSlots());

        Job updatedJob = jobRepository.save(job);
        return mapToJobResponse(updatedJob);
    }

    /**
     * FR-23: Cập nhật trạng thái công việc (OPEN, FILLED, COMPLETED, CLOSED)
     */
    @Transactional
    public Map<String, Object> updateJobStatus(Long id, String newStatus, Long employerId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với ID: " + id));

        if (!job.getEmployerId().equals(employerId)) {
            throw new ForbiddenException("Bạn không có quyền đổi trạng thái tin tuyển dụng này");
        }

        String currentStatus = job.getStatus();

        // Ràng buộc quy tắc nghiệp vụ mục 3.4: không cho quay ngược từ COMPLETED
        if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
            throw new BusinessRuleException("Tin tuyển dụng đã hoàn thành (COMPLETED), không thể thay đổi trạng thái nữa");
        }

        // Máy trạng thái hợp lệ
        boolean validTransition = false;
        if ("OPEN".equalsIgnoreCase(currentStatus)) {
            validTransition = "FILLED".equalsIgnoreCase(newStatus)
                    || "COMPLETED".equalsIgnoreCase(newStatus)
                    || "CLOSED".equalsIgnoreCase(newStatus);
        } else if ("FILLED".equalsIgnoreCase(currentStatus)) {
            validTransition = "COMPLETED".equalsIgnoreCase(newStatus)
                    || "CLOSED".equalsIgnoreCase(newStatus);
        } else if ("CLOSED".equalsIgnoreCase(currentStatus)) {
            validTransition = "OPEN".equalsIgnoreCase(newStatus);
        }

        if (!validTransition) {
            throw new BusinessRuleException("Chuyển trạng thái từ " + currentStatus + " sang " + newStatus + " không hợp lệ");
        }

        job.setStatus(newStatus.toUpperCase());
        Job savedJob = jobRepository.save(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", savedJob.getId());
        response.put("status", savedJob.getStatus());
        return response;
    }
}
