package com.example.sb.demo.service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    private static final String USER_SESSION_KEY = "user_id";
    private static final String USER_ROLE_KEY = "user_role";

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
        user.setPassword(request.getPassword()); // In production, hash the password
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());
        user.setDepartment(request.getDepartment());
        user.setYear(request.getYear());
        user.setRole("STUDENT"); // Default role for registration

        return userRepository.save(user);
    }

    public Optional<User> authenticateUser(LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .filter(user -> user.getPassword().equals(request.getPassword())); // In production, use proper password hashing
    }

    public void login(HttpSession session, User user) {
        session.setAttribute(USER_SESSION_KEY, user.getId());
        session.setAttribute(USER_ROLE_KEY, user.getRole());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

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

        // Add an 'active' field to User entity and implement this
        // user.setActive(active);
        return userRepository.save(user);
    }

    public void logout(HttpSession session) {
        session.removeAttribute(USER_SESSION_KEY);
        session.invalidate();
    }

    public Optional<User> getCurrentUser(HttpSession session) {
        Object userId = session.getAttribute(USER_SESSION_KEY);
        if (userId != null) {
            return userRepository.findById((Long) userId);
        }
        return Optional.empty();
    }

    public List<String> getAllDepartments() {
        return userRepository.findAll().stream()
                .map(User::getDepartment)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Repository doesn't track created date on User. For now return all users
     * so controllers that ask for date-range reports don't fail. This is a
     * lightweight compatibility shim.
     */
    public List<User> getUsersByDateRange(LocalDateTime start, LocalDateTime end) {
        return getAllUsers();
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }

    public boolean isStudent(User user) {
        return "STUDENT".equals(user.getRole());
    }
}