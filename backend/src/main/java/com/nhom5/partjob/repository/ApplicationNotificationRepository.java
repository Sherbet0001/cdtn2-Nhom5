package com.nhom5.partjob.repository;

import com.nhom5.partjob.domain.ApplicationNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationNotificationRepository extends JpaRepository<ApplicationNotification, Long> {

    @Query("""
            select n from ApplicationNotification n
            join fetch n.application a
            join fetch a.job j
            where n.user.id = :userId
            order by n.createdAt desc
            """)
    List<ApplicationNotification> findAllByUserId(@Param("userId") Long userId);

    Optional<ApplicationNotification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);
}
