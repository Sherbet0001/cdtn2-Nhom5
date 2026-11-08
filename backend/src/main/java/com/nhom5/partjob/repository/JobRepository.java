package com.nhom5.partjob.repository;

import com.nhom5.partjob.domain.Job;
import com.nhom5.partjob.domain.JobStatus;
import com.nhom5.partjob.domain.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("""
            select j from Job j
            left join j.location l
            where j.status = :status
              and (j.expiresAt is null or j.expiresAt >= :today)
              and (:query is null or lower(j.title) like lower(concat('%', :query, '%'))
                   or lower(j.description) like lower(concat('%', :query, '%'))
                   or lower(j.employer.profile.companyName) like lower(concat('%', :query, '%')))
              and (:locationId is null or l.id = :locationId)
              and (:jobType is null or j.jobType = :jobType)
            """)
    Page<Job> searchPublic(@Param("status") JobStatus status, @Param("today") LocalDate today,
                           @Param("query") String query, @Param("locationId") Long locationId,
                           @Param("jobType") JobType jobType, Pageable pageable);

    @Query("""
            select j from Job j
            left join fetch j.location
            join fetch j.employer e
            left join fetch e.profile
            where j.id = :id and j.status = :status
              and (j.expiresAt is null or j.expiresAt >= :today)
            """)
    Optional<Job> findPublicById(@Param("id") Long id, @Param("status") JobStatus status,
                                 @Param("today") LocalDate today);

    @Query("""
            select j from Job j
            left join fetch j.location
            join fetch j.employer e
            left join fetch e.profile
            where e.id = :employerId
            order by j.updatedAt desc
            """)
    java.util.List<Job> findAllByEmployerId(@Param("employerId") Long employerId);

    @Query("""
            select j from Job j
            left join fetch j.location
            join fetch j.employer e
            left join fetch e.profile
            where j.id = :jobId and e.id = :employerId
            """)
    Optional<Job> findByIdAndEmployerId(@Param("jobId") Long jobId, @Param("employerId") Long employerId);
}
