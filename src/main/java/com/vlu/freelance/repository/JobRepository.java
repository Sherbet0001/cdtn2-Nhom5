package com.vlu.freelance.repository;

import com.vlu.freelance.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Page<Job> findByEmployerId(Long employerId, Pageable pageable);

    /**
     * Tìm kiếm & lọc tin tuyển dụng (FR-07, FR-08, FR-09)
     * Kết hợp FULLTEXT Search MATCH ... AGAINST và LIKE fallback (Plan B buổi 4) cho từ khóa tiếng Việt ngắn (< 3 ký tự)
     * Lọc theo ngành nghề, địa điểm, và mức lương tối thiểu kèm đơn vị lương (nếu thiếu salaryUnit thì bỏ qua minSalary)
     */
    @Query(value = "SELECT * FROM tbl_jobs WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " MATCH(title, description) AGAINST(:keyword IN BOOLEAN MODE) OR " +
           " title LIKE CONCAT('%', :keyword, '%') OR " +
           " description LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:categoryId IS NULL OR category_id = :categoryId) AND " +
           "(:locationId IS NULL OR location_id = :locationId) AND " +
           "(:salaryUnit IS NULL OR :salaryUnit = '' OR salary_unit = :salaryUnit) AND " +
           "(:minSalary IS NULL OR :salaryUnit IS NULL OR :salaryUnit = '' OR salary_max >= :minSalary) AND " +
           "status = 'OPEN'",
           countQuery = "SELECT COUNT(*) FROM tbl_jobs WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " MATCH(title, description) AGAINST(:keyword IN BOOLEAN MODE) OR " +
           " title LIKE CONCAT('%', :keyword, '%') OR " +
           " description LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:categoryId IS NULL OR category_id = :categoryId) AND " +
           "(:locationId IS NULL OR location_id = :locationId) AND " +
           "(:salaryUnit IS NULL OR :salaryUnit = '' OR salary_unit = :salaryUnit) AND " +
           "(:minSalary IS NULL OR :salaryUnit IS NULL OR :salaryUnit = '' OR salary_max >= :minSalary) AND " +
           "status = 'OPEN'",
           nativeQuery = true)
    Page<Job> searchJobs(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         @Param("locationId") Long locationId,
                         @Param("minSalary") BigDecimal minSalary,
                         @Param("salaryUnit") String salaryUnit,
                         Pageable pageable);
}
