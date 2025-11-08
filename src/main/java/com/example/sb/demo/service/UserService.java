package com.example.sb.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sb.demo.dto.LoginRequest;
import com.example.sb.demo.dto.RegisterRequest;
import com.example.sb.demo.entity.User;
import com.example.sb.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class UserService {

    private final UserRepository userRepository;
    private static final String USER_SESSION_KEY = "user_id";
    private static final String USER_ROLE_KEY = "user_role";

  
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ==============================
    // STUDENT REGISTRATION
    // ==============================
    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // ⚠️ Should be hashed in production
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());
        user.setDepartment(request.getDepartment());
        user.setYear(request.getYear());
        user.setRole("STUDENT"); // Force role for normal registration

        return userRepository.save(user);
    }

    // ==============================
    // ADMIN REGISTRATION
    // ==============================
    @Transactional
    public User registerAdmin(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setDepartment(request.getDepartment());
        user.setYear(request.getYear());
        user.setRole("ADMIN"); // ✅ Important

        return userRepository.save(user);
    }


    // ==============================
    // LOGIN + SESSION MANAGEMENT
    // ==============================
    public Optional<User> authenticateUser(LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .filter(user -> user.getPassword().equals(request.getPassword()));
    }

//    public void login(HttpSession session, User user) {
//        session.setAttribute(USER_SESSION_KEY, user.getId());
//        session.setAttribute(USER_ROLE_KEY, user.getRole());
//    }
    
    public void login(HttpSession session, User user) {
        session.setAttribute("user", user);
        session.setAttribute("user_role", user.getRole());
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

//    public Optional<User> getCurrentUser(HttpSession session) {
//        Object userId = session.getAttribute(USER_SESSION_KEY);
//        if (userId != null) {
//            return userRepository.findById((Long) userId);
//        }
//        return Optional.empty();
//    }
    
    public Optional<User> getCurrentUser(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    // ==============================
    // ADMIN OPERATIONS
    // ==============================
    @Transactional
    public User updateUserRole(Long userId, String role, User admin) {
        if (!isAdmin(admin)) {
            throw new RuntimeException("Only admins can update user roles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getId().equals(admin.getId())) {
            throw new RuntimeException("Cannot modify your own role");
        }

        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserStatus(Long userId, boolean active, User admin) {
        if (!isAdmin(admin)) {
            throw new RuntimeException("Only admins can update user status");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getId().equals(admin.getId())) {
            throw new RuntimeException("Cannot modify your own status");
        }

        // If you add `active` field in User entity, uncomment this:
        // user.setActive(active);

        return userRepository.save(user);
    }

    // ==============================
    // UTILITIES
    // ==============================
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<String> getAllDepartments() {
        return userRepository.findAll().stream()
                .map(User::getDepartment)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<User> getUsersByDateRange(LocalDateTime start, LocalDateTime end) {
        return getAllUsers();
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }

    public boolean isStudent(User user) {
        return "STUDENT".equalsIgnoreCase(user.getRole());
    }
}
