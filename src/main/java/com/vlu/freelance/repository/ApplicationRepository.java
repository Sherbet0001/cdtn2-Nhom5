package com.vlu.freelance.repository;

import com.vlu.freelance.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Page<Application> findByJobId(Long jobId, Pageable pageable);
    Page<Application> findByStudentId(Long studentId, Pageable pageable);
    Optional<Application> findByJobIdAndStudentId(Long jobId, Long studentId);
    long countByJobIdAndStatus(Long jobId, String status);
}
