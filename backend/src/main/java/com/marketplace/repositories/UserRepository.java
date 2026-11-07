package com.marketplace.repositories;

import com.marketplace.entities.User;
import com.marketplace.entities.UserRole;
import com.marketplace.entities.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    long countByStatus(UserStatus status);

    @Override
    @NonNull
    Page<User> findAll(@Nullable Specification<User> spec, @NonNull Pageable pageable);
}

