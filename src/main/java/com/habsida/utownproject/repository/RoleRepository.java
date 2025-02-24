package com.habsida.utownproject.repository;

import com.habsida.utownproject.entity.Role;
import com.habsida.utownproject.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByType(RoleType type);
    Optional<Role> findByName(String name);
}
