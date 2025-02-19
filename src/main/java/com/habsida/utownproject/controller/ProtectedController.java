package com.habsida.utownproject.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    @GetMapping("/protected")
    public String protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Аутентифицированный пользователь: " + authentication.getName());
        System.out.println("Роли пользователя: " + authentication.getAuthorities());

        return "Доступ разрешён!";
    }
}
