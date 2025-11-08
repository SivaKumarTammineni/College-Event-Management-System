package com.example.sb.demo.controller;



import com.example.sb.demo.entity.User;
import com.example.sb.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/home")
    public String userHome(HttpSession session, Model model) {
        User user = userService.getCurrentUser(session)
                .orElseThrow(() -> new RuntimeException("You must log in first!"));

        model.addAttribute("user", user);
        model.addAttribute("isAdmin", userService.isAdmin(user));
        return "user/home"; // ✅ matches the new template
    }
}
