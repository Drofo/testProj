package com.habsida.utownproject.service;

import com.habsida.utownproject.entity.Role;
import com.habsida.utownproject.entity.User;
import com.habsida.utownproject.repository.RoleRepository;
import com.habsida.utownproject.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user, List<String> roleNames) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<Role> roles = new HashSet<>();

        if (roleNames != null && !roleNames.isEmpty()) {
            roles = roleNames.stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName)))
                    .collect(Collectors.toSet());
        } else {
            roles.add(roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Роль USER не найдена")));
        }

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        user.getRoles().addAll(roles);
        System.out.println("Создаётся пользователь: " + user.getPhoneNumber());
        System.out.println("Назначенные роли: " + user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumberWithRoles(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с номером: " + phoneNumber));
    }

    @Transactional
    public User updateUser(Long id, User updatedUser, List<String> newRoleNames) {
        User user = getUserById(id);

        if (updatedUser.getPhoneNumber() != null) user.setPhoneNumber(updatedUser.getPhoneNumber());
        if (updatedUser.getPassword() != null)
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        if (updatedUser.getFullName() != null) user.setFullName(updatedUser.getFullName());

        if (newRoleNames != null) {
            if (!newRoleNames.isEmpty()) {
                Set<Role> newRoles = newRoleNames.stream()
                        .map(roleName -> roleRepository.findByName(roleName)
                                .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName)))
                        .collect(Collectors.toSet());
                user.setRoles(newRoles);
            }
        }

        return userRepository.save(user);
    }

    public boolean userExists(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).isPresent();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user = getUserByPhoneNumber(phoneNumber);

        System.out.println("Загружен пользователь: " + user.getPhoneNumber());
        System.out.println("Роли из БД: " + user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getPhoneNumber())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                        .collect(Collectors.toSet()))
                .build();
    }
}
