package com.marketplace.services;

import com.marketplace.dtos.CreateJobRequest;
import com.marketplace.dtos.JobResponse;
import com.marketplace.dtos.PageResponse;
import com.marketplace.dtos.UpdateJobStatusRequest;
import com.marketplace.entities.ApplicationStatus;
import com.marketplace.entities.Category;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobApplication;
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
import com.marketplace.repositories.ReviewRepository;
import com.marketplace.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý nghiệp vụ Quản lý, Tìm kiếm và Chuyển trạng thái Tin tuyển dụng
 * (FR-07, FR-08, FR-09, FR-10, FR-17, FR-18, FR-20, FR-23).
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
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final DataSource dataSource;
    private final EntityManager entityManager;

    private volatile Boolean isMySqlCache;

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
        Profile employerProfile = profileRepository.findByUserId(employer.getId()).orElse(null);
        return mapToJobResponse(savedJob, employerProfile, null);
    }

    /**
     * Cập nhật nội dung tin tuyển dụng thuộc sở hữu của Nhà tuyển dụng (FR-18).
     * Không cho phép cập nhật khi tin đã ở trạng thái COMPLETED hoặc CLOSED, hoặc giảm slots nhỏ hơn số ứng viên đã ACCEPTED.
     *
     * @param employerId ID của Nhà tuyển dụng đang đăng nhập
     * @param jobId      ID tin tuyển dụng cần cập nhật
     * @param request    Dữ liệu cập nhật
     * @return Thông tin tin tuyển dụng sau khi cập nhật
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

        long acceptedCount = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.ACCEPTED);
        if (request.getSlots() < acceptedCount) {
            throw new AppException(
                    "SLOTS_EXCEEDED",
                    "Số lượng tuyển (slots) không được nhỏ hơn số lượng ứng viên đã được chấp nhận (" + acceptedCount + ")"
            );
        }

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
        Profile employerProfile = profileRepository.findByUserId(updatedJob.getEmployer().getId()).orElse(null);
        return mapToJobResponse(updatedJob, employerProfile, null);
    }

    /**
     * Chuyển trạng thái tin tuyển dụng theo quy tắc trạng thái Mục 5 (FR-23):
     * - OPEN -> FILLED -> COMPLETED
     * - NTD được chuyển OPEN hoặc FILLED -> CLOSED
     * - CLOSED và COMPLETED là trạng thái cuối, không quay ngược
     * - Chỉ NTD chủ tin được đổi trạng thái
     * - Khi chuyển sang COMPLETED, tự động tạo thông báo JOB_COMPLETED cho các Sinh viên đã ACCEPTED (FR-06).
     *
     * @param employerId ID của Nhà tuyển dụng đang đăng nhập
     * @param jobId      ID tin tuyển dụng
     * @param request    Trạng thái mới cần chuyển sang
     * @return Thông tin tin tuyển dụng sau khi đổi trạng thái
     * @throws AppException Mã lỗi JOB_NOT_FOUND (404), FORBIDDEN_RESOURCE (403), hoặc INVALID_STATUS_TRANSITION (409)
     */
    @Transactional
    public JobResponse changeJobStatus(Long employerId, Long jobId, UpdateJobStatusRequest request) {
        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        // QT-JOB (FR-23): Chỉ Nhà tuyển dụng sở hữu tin mới được phép đổi trạng thái tin
        if (!isJobOwnedByEmployer(job, employerId)) {
            throw new AppException("FORBIDDEN_RESOURCE", "Bạn chỉ có quyền đổi trạng thái đối với tin tuyển dụng do chính mình tạo ra");
        }

        JobStatus targetStatus;
        try {
            targetStatus = JobStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Trạng thái tin tuyển dụng không hợp lệ");
        }

        JobStatus currentStatus = job.getStatus();

        // QT-JOB (FR-23, Mục 5): Kiểm tra quy tắc chuyển trạng thái:
        // OPEN -> FILLED -> COMPLETED; OPEN/FILLED -> CLOSED; CLOSED và COMPLETED là trạng thái cuối, không quay ngược
        if (!isValidEmployerStatusTransition(currentStatus, targetStatus)) {
            throw new AppException(
                    "INVALID_STATUS_TRANSITION",
                    "Không thể chuyển trạng thái tin tuyển dụng từ " + currentStatus + " sang " + targetStatus
                            + " (Quy tắc: OPEN -> FILLED -> COMPLETED, hoặc OPEN/FILLED -> CLOSED; không quay ngược từ COMPLETED/CLOSED)"
            );
        }

        job.setStatus(targetStatus);
        Job savedJob = jobRepository.save(job);

        // QT-NOTIF (FR-23, FR-06): Khi chuyển sang COMPLETED, gửi thông báo JOB_COMPLETED cho các SV đã ACCEPTED
        if (targetStatus == JobStatus.COMPLETED) {
            List<JobApplication> acceptedApps = applicationRepository.findByJobIdAndStatusWithStudent(
                    jobId,
                    ApplicationStatus.ACCEPTED
            );
            notificationService.notifyAcceptedStudentsOnJobCompleted(savedJob, acceptedApps);
        }

        Profile employerProfile = profileRepository.findByUserId(savedJob.getEmployer().getId()).orElse(null);
        return mapToJobResponse(savedJob, employerProfile, employerId);
    }

    /**
     * Lấy danh sách tin tuyển dụng do chính Nhà tuyển dụng đang đăng nhập tạo ra (FR-18).
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
        List<JobResponse> content = mapJobListToResponses(jobPage.getContent());
        return PageResponse.fromPage(jobPage, content);
    }

    /**
     * Tìm kiếm và lọc danh sách việc làm đang mở tuyển (status = OPEN) kèm phân trang (FR-07, FR-08, FR-09).
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

        // FR-08: Khi có keyword trên MySQL 8.0, thực thi Native Query FULLTEXT MATCH(title, description) AGAINST(...)
        if (trimmedKeyword != null && isMySqlDatabase()) {
            try {
                Page<Job> fullTextPage = executeMySqlFullTextSearch(
                        trimmedKeyword,
                        categoryId,
                        locationId,
                        effectiveMinSalary,
                        parsedUnit != null ? parsedUnit.name() : null,
                        safePage,
                        safeSize
                );
                if (fullTextPage.hasContent()) {
                    List<JobResponse> mapped = mapJobListToResponses(fullTextPage.getContent());
                    return PageResponse.fromPage(fullTextPage, mapped);
                } else if (trimmedKeyword.length() >= 3) {
                    return PageResponse.fromPage(fullTextPage, Collections.emptyList());
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
        List<JobResponse> content = mapJobListToResponses(jobPage.getContent());

        return PageResponse.fromPage(jobPage, content);
    }

    /**
     * Xem thông tin chi tiết của một tin tuyển dụng (FR-10).
     * Chặn người dùng vãng lai hoặc sinh viên xem tin đã bị Admin ẩn (HIDDEN), trừ NTD chủ tin và ADMIN.
     */
    @Transactional(readOnly = true)
    public JobResponse getJobDetail(Long jobId, Long currentUserId, String currentUserRole) {
        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);
        if (job.getStatus() == JobStatus.HIDDEN && !isAdmin && !isJobOwnedByEmployer(job, currentUserId)) {
            throw new AppException("JOB_NOT_FOUND", "Tin tuyển dụng không tồn tại hoặc đã bị ẩn");
        }

        Profile employerProfile = profileRepository.findByUserId(job.getEmployer().getId()).orElse(null);
        return mapToJobResponse(job, employerProfile, currentUserId);
    }

    /**
     * Kiểm tra tin tuyển dụng có thuộc quyền sở hữu của Nhà tuyển dụng hay không.
     */
    public boolean isJobOwnedByEmployer(Job job, Long employerId) {
        return job != null
                && employerId != null
                && job.getEmployer() != null
                && job.getEmployer().getId().equals(employerId);
    }

    /**
     * Kiểm tra tin tuyển dụng có được phép chỉnh sửa nội dung hay không (chưa COMPLETED và chưa CLOSED).
     */
    public boolean canUpdateJob(Job job) {
        return job != null
                && job.getStatus() != JobStatus.COMPLETED
                && job.getStatus() != JobStatus.CLOSED;
    }

    /**
     * Kiểm tra tính hợp lệ của thao tác chuyển trạng thái tin tuyển dụng từ phía Nhà tuyển dụng (FR-23, Mục 5).
     */
    public boolean isValidEmployerStatusTransition(JobStatus currentStatus, JobStatus targetStatus) {
        if (currentStatus == null || targetStatus == null || currentStatus == targetStatus) {
            return false;
        }
        return switch (currentStatus) {
            case OPEN -> targetStatus == JobStatus.FILLED || targetStatus == JobStatus.CLOSED;
            case FILLED -> targetStatus == JobStatus.COMPLETED || targetStatus == JobStatus.CLOSED;
            case COMPLETED, CLOSED, HIDDEN -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private Page<Job> executeMySqlFullTextSearch(
            String keyword,
            Long categoryId,
            Long locationId,
            BigDecimal minSalary,
            String salaryUnit,
            int page,
            int size) {

        String whereClause = " FROM tbl_jobs j " +
                "WHERE j.status = 'OPEN' " +
                "AND MATCH(j.title, j.description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                "AND (:categoryId IS NULL OR j.category_id = :categoryId) " +
                "AND (:locationId IS NULL OR j.location_id = :locationId) " +
                "AND (:salaryUnit IS NULL OR :minSalary IS NULL OR (j.salary_unit = :salaryUnit AND j.salary_max >= :minSalary))";

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*)" + whereClause);
        bindFullTextParams(countQuery, keyword, categoryId, locationId, minSalary, salaryUnit);
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        Pageable pageable = PageRequest.of(page, size);
        if (totalElements == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query idQuery = entityManager.createNativeQuery(
                "SELECT j.id" + whereClause + " ORDER BY j.created_at DESC LIMIT :limit OFFSET :offset"
        );
        bindFullTextParams(idQuery, keyword, categoryId, locationId, minSalary, salaryUnit);
        idQuery.setParameter("limit", size);
        idQuery.setParameter("offset", page * size);

        List<Number> rawIds = idQuery.getResultList();
        List<Long> jobIds = rawIds.stream().map(Number::longValue).toList();
        if (jobIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
        }

        Map<Long, Job> jobsById = jobRepository.findAllWithDetailsByIdIn(jobIds)
                .stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));

        List<Job> orderedJobs = jobIds.stream()
                .map(jobsById::get)
                .filter(j -> j != null)
                .toList();

        return new PageImpl<>(orderedJobs, pageable, totalElements);
    }

    private void bindFullTextParams(
            Query query,
            String keyword,
            Long categoryId,
            Long locationId,
            BigDecimal minSalary,
            String salaryUnit) {
        query.setParameter("keyword", keyword);
        query.setParameter("categoryId", categoryId);
        query.setParameter("locationId", locationId);
        query.setParameter("minSalary", minSalary);
        query.setParameter("salaryUnit", salaryUnit);
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
        Boolean cached = this.isMySqlCache;
        if (cached != null) {
            return cached;
        }
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            boolean result = productName != null && productName.toLowerCase().contains("mysql");
            this.isMySqlCache = result;
            return result;
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

    private List<JobResponse> mapJobListToResponses(List<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> employerIds = jobs.stream()
                .map(j -> j.getEmployer().getId())
                .collect(Collectors.toSet());

        Map<Long, Profile> profilesByUserId = profileRepository.findAllByUserIdIn(employerIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));

        return jobs.stream()
                .map(job -> mapToJobResponse(job, profilesByUserId.get(job.getEmployer().getId()), null))
                .collect(Collectors.toList());
    }

    private JobResponse mapToJobResponse(Job job, Profile employerProfile, Long currentUserId) {
        User employer = job.getEmployer();

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
        String myApplicationStatus = null;
        Boolean hasReviewedEmployer = null;
        if (currentUserId != null) {
            JobApplication existingApp = applicationRepository
                    .findByJobIdAndStudentId(job.getId(), currentUserId)
                    .orElse(null);
            hasApplied = existingApp != null;
            if (existingApp != null) {
                myApplicationStatus = existingApp.getStatus().name();
            }
            hasReviewedEmployer = reviewRepository.existsByJobIdAndReviewerIdAndRevieweeId(
                    job.getId(),
                    currentUserId,
                    employer.getId()
            );
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
                .myApplicationStatus(myApplicationStatus)
                .hasReviewedEmployer(hasReviewedEmployer)
                .status(job.getStatus().name())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
