package com.habsida.utownproject.service;

import com.habsida.utownproject.entity.Role;
import com.habsida.utownproject.entity.RoleType;
import com.habsida.utownproject.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Role createRole(String roleName) {
        if (roleRepository.findByName(roleName).isPresent()) {
            throw new RuntimeException("Роль уже существует: " + roleName);
        }
        Role role = new Role(roleName);
        return roleRepository.save(role);
    }

    public Optional<Role> getRoleByName(String roleName) {
        return roleRepository.findByName(roleName);
    }

    public Optional<Role> getRoleByType(RoleType roleType) {
        return roleRepository.findByType(roleType);
    }

    @Transactional
    public void deleteRole(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));
        roleRepository.delete(role);
    }

    public boolean roleExists(String roleName) {
        return roleRepository.findByName(roleName).isPresent();
    }
}
