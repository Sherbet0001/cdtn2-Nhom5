package com.marketplace.repositories;

import com.marketplace.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @Query("SELECT p FROM Profile p JOIN FETCH p.user LEFT JOIN FETCH p.location WHERE p.user.id = :userId")
    Optional<Profile> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Profile p JOIN FETCH p.user LEFT JOIN FETCH p.location WHERE p.user.id IN :userIds")
    List<Profile> findAllByUserIdIn(@Param("userIds") Collection<Long> userIds);

    boolean existsByLocationId(Long locationId);

    /**
     * Đếm số hồ sơ ứng tuyển của một sinh viên vào các tin tuyển dụng thuộc sở hữu của một nhà tuyển dụng.
     */
    @Query(
            "SELECT COUNT(a) FROM JobApplication a " +
            "WHERE a.student.id = :studentId AND a.job.employer.id = :employerId"
    )
    long countStudentApplicationsForEmployer(
            @Param("studentId") Long studentId,
            @Param("employerId") Long employerId
    );
}
