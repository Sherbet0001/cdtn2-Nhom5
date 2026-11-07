package com.marketplace.repositories;

import com.marketplace.entities.Job;
import com.marketplace.entities.JobStatus;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"employer", "category", "location"})
    Page<Job> findAll(@Nullable Specification<Job> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"employer", "category", "location"})
    Page<Job> findByEmployerId(Long employerId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByLocationId(Long locationId);

    long countByStatus(JobStatus status);

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
}
