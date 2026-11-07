package com.marketplace.repositories;

import com.marketplace.entities.ApplicationStatus;
import com.marketplace.entities.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);

    Optional<JobApplication> findByJobIdAndStudentId(Long jobId, Long studentId);

    long countByJobId(Long jobId);

    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);

    @Query(
            "SELECT a FROM JobApplication a " +
            "JOIN FETCH a.job j " +
            "JOIN FETCH j.employer " +
            "JOIN FETCH a.student " +
            "WHERE a.job.id = :jobId " +
            "ORDER BY a.appliedAt DESC"
    )
    List<JobApplication> findByJobIdWithStudentOrderByAppliedAtDesc(@Param("jobId") Long jobId);

    @Query(
            "SELECT a FROM JobApplication a " +
            "JOIN FETCH a.job j " +
            "JOIN FETCH j.employer " +
            "JOIN FETCH a.student " +
            "WHERE a.id = :id"
    )
    Optional<JobApplication> findWithDetailsById(@Param("id") Long id);
}

