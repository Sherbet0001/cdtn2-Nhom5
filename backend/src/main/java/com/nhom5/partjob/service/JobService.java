package com.nhom5.partjob.service;

import com.nhom5.partjob.domain.*;
import com.nhom5.partjob.dto.JobDtos.*;
import com.nhom5.partjob.exception.ApiException;
import com.nhom5.partjob.repository.JobRepository;
import com.nhom5.partjob.repository.LocationRepository;
import com.nhom5.partjob.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    public JobService(JobRepository jobRepository, UserRepository userRepository,
                      LocationRepository locationRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> search(String query, Long locationId, JobType jobType, int page, int size) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pagination = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedAt"));
        return jobRepository.searchPublic(JobStatus.OPEN, LocalDate.now(), normalizedQuery, locationId, jobType,
                pagination).map(JobService::toResponse);
    }

    @Transactional(readOnly = true)
    public JobResponse getPublic(Long jobId) {
        return jobRepository.findPublicById(jobId, JobStatus.OPEN, LocalDate.now())
                .map(JobService::toResponse)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tin tuyển dụng đang mở"));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getMine(Long employerId) {
        return jobRepository.findAllByEmployerId(employerId).stream().map(JobService::toResponse).toList();
    }

    @Transactional
    public JobResponse create(Long employerId, JobRequest request) {
        User employer = userRepository.findById(employerId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản nhà tuyển dụng"));
        Job job = new Job(employer, resolveLocation(request.locationId()), request.title().trim(),
                request.description().trim(), blankToNull(request.requirements()), blankToNull(request.benefits()),
                request.jobType(), request.salaryMin(), request.salaryMax(), request.expiresAt());
        validateSalaryRange(request);
        validateExpiry(request.expiresAt());
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse update(Long employerId, Long jobId, JobRequest request) {
        Job job = findOwnedJob(employerId, jobId);
        validateSalaryRange(request);
        validateExpiry(request.expiresAt());
        job.update(resolveLocation(request.locationId()), request.title().trim(), request.description().trim(),
                blankToNull(request.requirements()), blankToNull(request.benefits()), request.jobType(),
                request.salaryMin(), request.salaryMax(), request.expiresAt());
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse updateStatus(Long employerId, Long jobId, JobStatus status) {
        Job job = findOwnedJob(employerId, jobId);
        job.setStatus(status);
        return toResponse(jobRepository.save(job));
    }

    private Job findOwnedJob(Long employerId, Long jobId) {
        return jobRepository.findByIdAndEmployerId(jobId, employerId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tin tuyển dụng"));
    }

    private Location resolveLocation(Long locationId) {
        if (locationId == null) return null;
        return locationRepository.findById(locationId)
                .orElseThrow(() -> ApiException.badRequest("Khu vực đã chọn không tồn tại"));
    }

    private static void validateSalaryRange(JobRequest request) {
        if (request.salaryMin() != null && request.salaryMax() != null
                && request.salaryMin().compareTo(request.salaryMax()) > 0) {
            throw ApiException.badRequest("Mức lương tối thiểu không được lớn hơn mức tối đa");
        }
    }

    private static void validateExpiry(LocalDate expiresAt) {
        if (expiresAt != null && expiresAt.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Hạn tuyển dụng phải từ hôm nay trở đi");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static JobResponse toResponse(Job job) {
        Location location = job.getLocation();
        Profile profile = job.getEmployer().getProfile();
        String companyName = profile == null || profile.getCompanyName() == null || profile.getCompanyName().isBlank()
                ? (profile == null ? "Nhà tuyển dụng" : profile.getFullName()) : profile.getCompanyName();
        LocationResponse locationResponse = location == null ? null
                : new LocationResponse(location.getId(), location.getCity(), location.getDistrict());
        return new JobResponse(job.getId(), job.getTitle(), job.getDescription(), job.getRequirements(),
                job.getBenefits(), job.getJobType(), job.getSalaryMin(), job.getSalaryMax(), job.getStatus(),
                job.getExpiresAt(), job.getPostedAt(), job.getUpdatedAt(), job.getEmployer().getId(),
                companyName, locationResponse);
    }
}
