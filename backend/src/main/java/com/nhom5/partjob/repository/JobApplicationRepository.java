package com.nhom5.partjob.repository;

import com.nhom5.partjob.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);

    @Query("""
            select a from JobApplication a
            join fetch a.job j
            join fetch a.student s
            left join fetch s.profile
            where s.id = :studentId
            order by a.appliedAt desc
            """)
    List<JobApplication> findAllByStudentId(@Param("studentId") Long studentId);

    @Query("""
            select a from JobApplication a
            join fetch a.job j
            join fetch a.student s
            left join fetch s.profile
            where j.id = :jobId and j.employer.id = :employerId
            order by a.appliedAt desc
            """)
    List<JobApplication> findAllByJobAndEmployer(@Param("jobId") Long jobId,
                                                  @Param("employerId") Long employerId);

    @Query("""
            select a from JobApplication a
            join fetch a.job j
            join fetch j.employer e
            left join fetch e.profile
            join fetch a.student s
            left join fetch s.profile
            where e.id = :employerId
            order by a.appliedAt desc
            """)
    List<JobApplication> findAllByEmployer(@Param("employerId") Long employerId);

    @Query("""
            select a from JobApplication a
            join fetch a.job j
            join fetch j.employer e
            join fetch a.student s
            left join fetch s.profile
            where a.id = :applicationId
            """)
    Optional<JobApplication> findForCvDownload(@Param("applicationId") Long applicationId);

    @Query("""
            select a from JobApplication a
            join fetch a.job j
            join fetch j.employer e
            join fetch a.student s
            left join fetch s.profile
            where a.id = :applicationId and e.id = :employerId
            """)
    Optional<JobApplication> findByIdAndEmployerId(@Param("applicationId") Long applicationId,
                                                    @Param("employerId") Long employerId);
}
