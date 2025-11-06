package com.example.sb.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sb.demo.dto.LoginRequest;
import com.example.sb.demo.dto.RegisterRequest;
import com.example.sb.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final UserService userService;

    // ✅ Constructor-based dependency injection (best practice)
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ======================================================
    // LOGIN
    // ======================================================
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest loginRequest,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            return userService.authenticateUser(loginRequest)
                    .map(user -> {
                        // ✅ Save user info in session
                        userService.login(session, user);

                        redirectAttributes.addFlashAttribute("successMessage",
                                "Welcome back, " + user.getFullName() + "!");

                        // ✅ Redirect based on role
                        if (userService.isAdmin(user)) {
                            return "redirect:/admin/dashboard";
                        } else {
                            return "redirect:/user/home"; // ✅ Correct student redirect
                        }
                    })
                    .orElseThrow(() -> new RuntimeException("Invalid username or password."));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // ======================================================
    // ADMIN REGISTRATION
    // ======================================================
    @GetMapping("/admin/register")
    public String adminRegisterPage() {
        return "auth/admin-register";
    }

    @PostMapping("/admin/register")
    public String registerAdmin(@ModelAttribute RegisterRequest registerRequest,
                                RedirectAttributes redirectAttributes) {
        try {
            // ✅ Force role as ADMIN (even if the form doesn’t send it)
            registerRequest.setRole("ADMIN");
            userService.registerAdmin(registerRequest);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Admin registered successfully! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/register"; // ✅ Corrected path
        }
    }

    // ======================================================
    // STUDENT REGISTRATION
    // ======================================================
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest registerRequest,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(registerRequest);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    // ======================================================
    // LOGOUT
    // ======================================================
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        userService.logout(session);
        redirectAttributes.addFlashAttribute("successMessage",
                "You have been logged out successfully.");
        return "redirect:/login";
    }
}
