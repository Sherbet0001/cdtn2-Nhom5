package com.marketplace.repositories;

import com.marketplace.entities.Job;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"employer", "category", "location"})
    Page<Job> findAll(@Nullable Specification<Job> spec, @NonNull Pageable pageable);

    @Query(
            "SELECT j FROM Job j " +
            "JOIN FETCH j.employer " +
            "JOIN FETCH j.category " +
            "JOIN FETCH j.location " +
            "WHERE j.id = :id"
    )
    Optional<Job> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT j FROM Job j " +
            "JOIN FETCH j.employer " +
            "JOIN FETCH j.category " +
            "JOIN FETCH j.location " +
            "WHERE j.id = :id"
    )
    Optional<Job> findByIdForUpdate(@Param("id") Long id);

    @Query(
            "SELECT j FROM Job j " +
            "JOIN FETCH j.employer " +
            "JOIN FETCH j.category " +
            "JOIN FETCH j.location " +
            "WHERE j.id IN :ids"
    )
    List<Job> findAllWithDetailsByIdIn(@Param("ids") Collection<Long> ids);

    @Query(
            value = "SELECT j FROM Job j " +
                    "JOIN FETCH j.employer " +
                    "JOIN FETCH j.category " +
                    "JOIN FETCH j.location " +
                    "WHERE j.employer.id = :employerId",
            countQuery = "SELECT COUNT(j) FROM Job j WHERE j.employer.id = :employerId"
    )
    Page<Job> findByEmployerId(@Param("employerId") Long employerId, Pageable pageable);

    /**
     * FR-08 & FR-09: Tìm kiếm từ khóa bằng MySQL Native Query MATCH(title, description) AGAINST(...)
     * kết hợp bộ lọc categoryId, locationId và minSalary + salaryUnit trên các tin đang OPEN.
     * Lưu ý: Đặt ORDER BY j.created_at DESC trực tiếp trong câu truy vấn gốc và truyền PageRequest không kèm Sort
     * để tránh lỗi phân tích alias của Spring Data JPA khi kết hợp Native Query với countQuery.
     */
    @Query(
            value = "SELECT j.* FROM tbl_jobs j " +
                    "WHERE j.status = 'OPEN' " +
                    "AND MATCH(j.title, j.description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                    "AND (:categoryId IS NULL OR j.category_id = :categoryId) " +
                    "AND (:locationId IS NULL OR j.location_id = :locationId) " +
                    "AND (:salaryUnit IS NULL OR :minSalary IS NULL OR (j.salary_unit = :salaryUnit AND j.salary_max >= :minSalary)) " +
                    "ORDER BY j.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM tbl_jobs j " +
                    "WHERE j.status = 'OPEN' " +
                    "AND MATCH(j.title, j.description) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                    "AND (:categoryId IS NULL OR j.category_id = :categoryId) " +
                    "AND (:locationId IS NULL OR j.location_id = :locationId) " +
                    "AND (:salaryUnit IS NULL OR :minSalary IS NULL OR (j.salary_unit = :salaryUnit AND j.salary_max >= :minSalary))",
            nativeQuery = true
    )
    Page<Job> searchOpenJobsFullText(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("locationId") Long locationId,
            @Param("minSalary") BigDecimal minSalary,
            @Param("salaryUnit") String salaryUnit,
            Pageable pageable
    );
}
