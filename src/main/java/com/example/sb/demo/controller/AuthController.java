package com.example.sb.demo.controller;

import com.example.sb.demo.dto.LoginRequest;
import com.example.sb.demo.dto.RegisterRequest;
import com.example.sb.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    // ✅ Constructor-based dependency injection
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest loginRequest,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            userService.authenticateUser(loginRequest)
                    .ifPresentOrElse(
                            user -> {
                                userService.login(session, user);
                                redirectAttributes.addFlashAttribute("successMessage",
                                        "Welcome back, " + user.getFullName() + "!");
                            },
                            () -> {
                                throw new RuntimeException("Invalid credentials");
                            }
                    );
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest registerRequest,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(registerRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        userService.logout(session);
        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully.");
        return "redirect:/login";
    }
}
