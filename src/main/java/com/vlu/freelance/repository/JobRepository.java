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

    @Query(value = "SELECT * FROM tbl_jobs WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR MATCH(title, description) AGAINST(:keyword IN BOOLEAN MODE)) AND " +
           "(:categoryId IS NULL OR category_id = :categoryId) AND " +
           "(:locationId IS NULL OR location_id = :locationId) AND " +
           "(:salaryUnit IS NULL OR :salaryUnit = '' OR salary_unit = :salaryUnit) AND " +
           "(:minSalary IS NULL OR :salaryUnit IS NULL OR :salaryUnit = '' OR salary_max >= :minSalary) AND " +
           "status = 'OPEN'",
           countQuery = "SELECT COUNT(*) FROM tbl_jobs WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR MATCH(title, description) AGAINST(:keyword IN BOOLEAN MODE)) AND " +
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
