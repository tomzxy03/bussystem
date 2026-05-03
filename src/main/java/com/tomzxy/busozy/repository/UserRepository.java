package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    @EntityGraph(attributePaths = "company")
    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR u.status = :status)
              AND (:role IS NULL OR u.role = :role)
              AND (:banned IS NULL OR u.isBanned = :banned)
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchAdminUsers(
            @Param("search") String search,
            @Param("status") com.tomzxy.busozy.common.enums.UserStatus status,
            @Param("role") com.tomzxy.busozy.common.enums.UserRole role,
            @Param("banned") Boolean banned,
            Pageable pageable);
}
