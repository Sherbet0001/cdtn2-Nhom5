package com.marketplace.services;

import com.marketplace.dtos.CreateJobRequest;
import com.marketplace.dtos.JobResponse;
import com.marketplace.dtos.PageResponse;
import com.marketplace.entities.ApplicationStatus;
import com.marketplace.entities.Category;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobStatus;
import com.marketplace.entities.Location;
import com.marketplace.entities.Profile;
import com.marketplace.entities.SalaryUnit;
import com.marketplace.entities.User;
import com.marketplace.entities.UserRole;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.ApplicationRepository;
import com.marketplace.repositories.CategoryRepository;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.LocationRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý nghiệp vụ Quản lý và Tìm kiếm Tin tuyển dụng (FR-07, FR-08, FR-09, FR-10, FR-17, FR-18, FR-20).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ApplicationRepository applicationRepository;
    private final DataSource dataSource;

    /**
     * Đăng tin tuyển dụng việc làm part-time mới cho Nhà tuyển dụng (FR-17).
     *
     * @param employerId ID của Nhà tuyển dụng đang đăng nhập
     * @param request    Dữ liệu tin tuyển dụng mới
     * @return Thông tin tin tuyển dụng vừa tạo với trạng thái mặc định OPEN
     * @throws AppException Mã lỗi BAD_REQUEST nếu khoảng lương/ngày không hợp lệ, hoặc CATEGORY_NOT_FOUND / LOCATION_NOT_FOUND
     */
    @Transactional
    public JobResponse createJob(Long employerId, CreateJobRequest request) {
        User employer = userRepository.findById(employerId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "Không tìm thấy tài khoản nhà tuyển dụng"));

        // QT-JOB (FR-17): Chỉ tài khoản EMPLOYER mới được phép đăng tin tuyển dụng
        if (employer.getRole() != UserRole.EMPLOYER) {
            throw new AppException("FORBIDDEN_RESOURCE", "Chỉ Nhà tuyển dụng mới có quyền đăng tin tuyển dụng");
        }

        validateJobBusinessConstraints(request);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Danh mục ngành nghề không tồn tại"));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new AppException("LOCATION_NOT_FOUND", "Địa điểm làm việc không tồn tại"));

        SalaryUnit salaryUnit = parseSalaryUnit(request.getSalaryUnit());

        Job job = Job.builder()
                .employer(employer)
                .category(category)
                .location(location)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryUnit(salaryUnit)
                .workSchedule(request.getWorkSchedule().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .slots(request.getSlots())
                .status(JobStatus.OPEN)
                .build();

        Job savedJob = jobRepository.save(job);
        return mapToJobResponse(savedJob, null);
    }

    /**
     * Cập nhật nội dung tin tuyển dụng thuộc sở hữu của Nhà tuyển dụng (FR-18).
     * Không cho phép cập nhật khi tin đã ở trạng thái COMPLETED hoặc CLOSED.
     *
     * @param employerId ID của Nhà tuyển dụng đang đăng nhập
     * @param jobId      ID tin tuyển dụng cần cập nhật
     * @param request    Dữ liệu cập nhật
     * @return Thông tin tin tuyển dụng sau khi cập nhật
     * @throws AppException Mã lỗi JOB_NOT_FOUND (404), FORBIDDEN_RESOURCE (403) nếu không phải chủ tin, hoặc INVALID_STATUS_TRANSITION (409) nếu tin đã COMPLETED
     */
    @Transactional
    public JobResponse updateJob(Long employerId, Long jobId, CreateJobRequest request) {
        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        // QT-JOB (FR-18): Kiểm tra quyền sở hữu tin tuyển dụng (Resource Ownership Check)
        if (!isJobOwnedByEmployer(job, employerId)) {
            throw new AppException("FORBIDDEN_RESOURCE", "Bạn chỉ được phép chỉnh sửa tin tuyển dụng do chính mình đăng tải");
        }

        // QT-JOB (FR-18): Chặn chỉnh sửa nội dung khi tin đã hoàn thành (COMPLETED) hoặc đã đóng (CLOSED)
        if (!canUpdateJob(job)) {
            throw new AppException("INVALID_STATUS_TRANSITION", "Không thể chỉnh sửa tin tuyển dụng đã hoàn thành (COMPLETED) hoặc đã đóng (CLOSED)");
        }

        validateJobBusinessConstraints(request);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Danh mục ngành nghề không tồn tại"));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new AppException("LOCATION_NOT_FOUND", "Địa điểm làm việc không tồn tại"));

        job.setCategory(category);
        job.setLocation(location);
        job.setTitle(request.getTitle().trim());
        job.setDescription(request.getDescription().trim());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSalaryUnit(parseSalaryUnit(request.getSalaryUnit()));
        job.setWorkSchedule(request.getWorkSchedule().trim());
        job.setStartDate(request.getStartDate());
        job.setEndDate(request.getEndDate());
        job.setSlots(request.getSlots());

        Job updatedJob = jobRepository.save(job);
        return mapToJobResponse(updatedJob, null);
    }

    /**
     * Lấy danh sách tin tuyển dụng do chính Nhà tuyển dụng đang đăng nhập tạo ra (FR-20).
     *
     * @param employerId ID của Nhà tuyển dụng
     * @param page       Số trang (bắt đầu từ 0)
     * @param size       Số bản ghi mỗi trang
     * @return Trang danh sách tin tuyển dụng của NTD
     */
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> getEmployerJobs(Long employerId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Job> jobPage = jobRepository.findByEmployerId(employerId, pageable);
        List<JobResponse> content = jobPage.getContent()
                .stream()
                .map(job -> mapToJobResponse(job, null))
                .collect(Collectors.toList());

        return PageResponse.fromPage(jobPage, content);
    }

    /**
     * Tìm kiếm và lọc danh sách việc làm đang mở tuyển (status = OPEN) kèm phân trang (FR-07, FR-08, FR-09).
     * - Từ khóa (keyword): Ưu tiên thực thi Native Query FULLTEXT MATCH(title, description) AGAINST(...) trên MySQL 8.0;
     *   tự động chuyển sang Specification LIKE khi chạy trên CSDL nhúng H2 hoặc từ khóa ngắn.
     * - Mức lương tối thiểu (minSalary): Chỉ áp dụng khi đi kèm đơn vị tính lương (salaryUnit).
     *
     * @param keyword    Từ khóa tìm kiếm trên tiêu đề và mô tả
     * @param categoryId ID danh mục ngành nghề
     * @param locationId ID địa điểm làm việc
     * @param minSalary  Mức lương tối thiểu mong muốn
     * @param salaryUnit Đơn vị tính lương đi kèm minSalary (HOUR, DAY, MONTH, TASK)
     * @param page       Trang hiện tại (mặc định 0)
     * @param size       Kích thước trang (mặc định 10)
     * @param sortParam  Chuỗi sắp xếp (ví dụ: createdAt,desc)
     * @return Trang kết quả việc làm chuẩn { content, page, size, totalElements, totalPages }
     */
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> searchOpenJobs(
            String keyword,
            Long categoryId,
            Long locationId,
            BigDecimal minSalary,
            String salaryUnit,
            int page,
            int size,
            String sortParam) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        // QT-JOB (Mục 5): minSalary bắt buộc phải đi kèm salaryUnit, nếu thiếu salaryUnit thì bỏ qua minSalary
        SalaryUnit parsedUnit = null;
        BigDecimal effectiveMinSalary = null;
        if (minSalary != null && salaryUnit != null && !salaryUnit.trim().isEmpty()) {
            parsedUnit = parseSalaryUnit(salaryUnit);
            effectiveMinSalary = minSalary;
        }

        // FR-08: Khi có keyword trên MySQL 8.0, ưu tiên thực thi Native Query FULLTEXT MATCH ... AGAINST trước
        if (trimmedKeyword != null && isMySqlDatabase()) {
            try {
                Pageable nativePageable = PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.DESC, "created_at")
                );
                Page<Job> fullTextPage = jobRepository.searchOpenJobsFullText(
                        trimmedKeyword,
                        categoryId,
                        locationId,
                        effectiveMinSalary,
                        parsedUnit != null ? parsedUnit.name() : null,
                        nativePageable
                );
                if (fullTextPage.hasContent()) {
                    List<JobResponse> mapped = fullTextPage.getContent()
                            .stream()
                            .map(job -> mapToJobResponse(job, null))
                            .collect(Collectors.toList());
                    return PageResponse.fromPage(fullTextPage, mapped);
                }
            } catch (Exception ignored) {
                // Tự động chuyển sang JPA Specification nếu từ khóa đặc biệt không khớp cú pháp FULLTEXT
            }
        }

        Pageable jpaPageable = PageRequest.of(safePage, safeSize, parseSort(sortParam));
        final SalaryUnit finalUnit = parsedUnit;
        final BigDecimal finalMinSalary = effectiveMinSalary;

        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // QT-JOB (FR-07, FR-09): Chỉ hiển thị các tin đang mở tuyển (status = OPEN)
            predicates.add(cb.equal(root.get("status"), JobStatus.OPEN));

            if (trimmedKeyword != null) {
                String pattern = "%" + trimmedKeyword.toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }

            if (finalMinSalary != null && finalUnit != null) {
                predicates.add(cb.equal(root.get("salaryUnit"), finalUnit));
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), finalMinSalary));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Job> jobPage = jobRepository.findAll(spec, jpaPageable);
        List<JobResponse> content = jobPage.getContent()
                .stream()
                .map(job -> mapToJobResponse(job, null))
                .collect(Collectors.toList());

        return PageResponse.fromPage(jobPage, content);
    }

    /**
     * Xem thông tin chi tiết của một tin tuyển dụng (FR-10).
     *
     * @param jobId         ID tin tuyển dụng
     * @param currentUserId ID người dùng đang xem (có thể null nếu chưa đăng nhập)
     * @return Thông tin chi tiết tin tuyển dụng kèm trạng thái đã ứng tuyển (hasApplied)
     * @throws AppException Mã lỗi JOB_NOT_FOUND (404) nếu không tìm thấy tin
     */
    @Transactional(readOnly = true)
    public JobResponse getJobDetail(Long jobId, Long currentUserId) {
        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        return mapToJobResponse(job, currentUserId);
    }

    /**
     * Kiểm tra tin tuyển dụng có thuộc quyền sở hữu của Nhà tuyển dụng hay không.
     *
     * @param job        Thực thể tin tuyển dụng
     * @param employerId ID của Nhà tuyển dụng
     * @return true nếu job.employer.id == employerId
     */
    public boolean isJobOwnedByEmployer(Job job, Long employerId) {
        return job != null && job.getEmployer() != null && job.getEmployer().getId().equals(employerId);
    }

    /**
     * Kiểm tra tin tuyển dụng có được phép chỉnh sửa nội dung hay không (chưa COMPLETED và chưa CLOSED).
     *
     * @param job Thực thể tin tuyển dụng
     * @return true nếu trạng thái khác COMPLETED và CLOSED
     */
    public boolean canUpdateJob(Job job) {
        return job != null
                && job.getStatus() != JobStatus.COMPLETED
                && job.getStatus() != JobStatus.CLOSED;
    }

    private void validateJobBusinessConstraints(CreateJobRequest request) {
        if (request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new AppException("BAD_REQUEST", "Mức lương tối thiểu không được lớn hơn mức lương tối đa");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new AppException("BAD_REQUEST", "Ngày bắt đầu công việc không được sau ngày kết thúc");
        }
    }

    private SalaryUnit parseSalaryUnit(String rawUnit) {
        try {
            return SalaryUnit.valueOf(rawUnit.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Đơn vị tính lương chỉ chấp nhận HOUR, DAY, MONTH hoặc TASK");
        }
    }

    private boolean isMySqlDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("mysql");
        } catch (Exception ex) {
            return false;
        }
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        if (!List.of("createdAt", "salaryMin", "salaryMax", "startDate", "id").contains(property)) {
            property = "createdAt";
        }
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private JobResponse mapToJobResponse(Job job, Long currentUserId) {
        User employer = job.getEmployer();
        Profile employerProfile = profileRepository.findByUserId(employer.getId()).orElse(null);

        String employerFullName = employerProfile != null ? employerProfile.getFullName() : employer.getEmail();
        String companyName = (employerProfile != null && employerProfile.getCompanyName() != null)
                ? employerProfile.getCompanyName()
                : employerFullName;

        Category category = job.getCategory();
        Location location = job.getLocation();
        String locationName = location.getDistrict() + ", " + location.getCity();

        long applicantCount = applicationRepository.countByJobId(job.getId());
        long acceptedCount = applicationRepository.countByJobIdAndStatus(job.getId(), ApplicationStatus.ACCEPTED);

        Boolean hasApplied = null;
        if (currentUserId != null) {
            hasApplied = applicationRepository.existsByJobIdAndStudentId(job.getId(), currentUserId);
        }

        return JobResponse.builder()
                .id(job.getId())
                .jobId(job.getId())
                .employerId(employer.getId())
                .employerEmail(employer.getEmail())
                .employerFullName(employerFullName)
                .companyName(companyName)
                .categoryId(category.getId())
                .categoryName(category.getName())
                .locationId(location.getId())
                .locationName(locationName)
                .title(job.getTitle())
                .description(job.getDescription())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryUnit(job.getSalaryUnit().name())
                .workSchedule(job.getWorkSchedule())
                .startDate(job.getStartDate())
                .endDate(job.getEndDate())
                .slots(job.getSlots())
                .applicantCount(applicantCount)
                .acceptedCount(acceptedCount)
                .hasApplied(hasApplied)
                .status(job.getStatus().name())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}

