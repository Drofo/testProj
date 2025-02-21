package com.habsida.utownproject.repository;

import com.habsida.utownproject.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumberWithRoles(@Param("phoneNumber") String phoneNumber);
}
