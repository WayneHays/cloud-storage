package com.waynehays.cloudfilestorage.repository;

import com.waynehays.cloudfilestorage.entity.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @NotNull
    Page<User> findAll(@NotNull Pageable pageable);
}
