package com.habsida.utownproject.controller;

import com.habsida.utownproject.entity.User;
import com.habsida.utownproject.entity.Role;
import com.habsida.utownproject.security.JwtService;
import com.habsida.utownproject.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserDetailsService userDetailsService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public ResponseEntity<String> loginPage() {
        return ResponseEntity.status(401).body("This is an API, use /api/auth/login for authentication.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            System.out.println("Trying to login with phoneNumber: " + request.getPhoneNumber());
            System.out.println("Password entered: " + request.getPassword());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
            );

            User user = userService.getUserByPhoneNumber(request.getPhoneNumber());

            Set<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            String token = jwtService.generateToken(user.getPhoneNumber(), roles);

            return ResponseEntity.ok(new AuthResponse(token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(403).body("Invalid phone number or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userService.userExists(request.getPhoneNumber())) {
            return ResponseEntity.status(400).body("Пользователь с таким номером уже существует");
        }

        User newUser = new User();
        newUser.setPhoneNumber(request.getPhoneNumber());
        newUser.setPassword(request.getPassword());
        newUser.setFullName(request.getFullName());

        List<String> roles = request.getRoles() != null && !request.getRoles().isEmpty()
                ? request.getRoles()
                : List.of("USER");

        User createdUser = userService.createUser(newUser, roles);

        Set<String> roleNames = createdUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(createdUser.getPhoneNumber(), roleNames);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    public static class AuthRequest {
        private String phoneNumber;
        private String password;
        private String fullName;
        private List<String> roles;

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public static class AuthResponse {
        private String token;

        public AuthResponse(String token) { this.token = token; }

        public String getToken() { return token; }
    }
}
