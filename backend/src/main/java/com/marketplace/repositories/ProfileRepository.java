package com.marketplace.repositories;

import com.marketplace.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @Query("SELECT p FROM Profile p JOIN FETCH p.user LEFT JOIN FETCH p.location WHERE p.user.id = :userId")
    Optional<Profile> findByUserId(@Param("userId") Long userId);
}

