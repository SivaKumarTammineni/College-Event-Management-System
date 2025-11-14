package com.example.sb.demo.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.Registration;
import com.example.sb.demo.entity.User;
import com.example.sb.demo.service.EventService;
import com.example.sb.demo.service.RegistrationService;
import com.example.sb.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final EventService eventService;
    private final RegistrationService registrationService;

    // ✅ Constructor Injection
    public AdminController(UserService userService,
                           EventService eventService,
                           RegistrationService registrationService) {
        this.userService = userService;
        this.eventService = eventService;
        this.registrationService = registrationService;
    }

    /** ✅ Ensure only admins can access routes */
    private User getCurrentAdmin(HttpSession session) {
        Optional<User> userOpt = userService.getCurrentUser(session);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("You are not logged in. Please login as admin.");
        }

        User user = userOpt.get();

        if (!userService.isAdmin(user)) {
            throw new RuntimeException("Access denied. Admin privileges required.");
        }

        session.setAttribute("isAdmin", true);
        return user;
    }

    /** ✅ Common model attributes for admin pages */
    @ModelAttribute
    public void addCommonAttributes(Model model, HttpSession session) {
        try {
            User admin = getCurrentAdmin(session);
            model.addAttribute("user", admin);
            model.addAttribute("isAdmin", true);
        } catch (Exception ignored) {
            // unauthenticated users will be redirected
        }
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    /** ✅ Admin Dashboard */
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        getCurrentAdmin(session);

        List<Event> allEvents = eventService.getAllEvents();
        List<Event> upcomingEvents = eventService.getUpcomingEvents();
        List<Event> pastEvents = eventService.getPastEvents();
        List<Registration> allRegistrations = registrationService.getAllRegistrations();
        List<User> allUsers = userService.getAllUsers();

        // Registration statistics
        Map<String, Long> registrationStats = allRegistrations.stream()
                .collect(Collectors.groupingBy(Registration::getStatus, Collectors.counting()));

        // User statistics
        Map<String, Long> userStats = allUsers.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));

        // ✅ Summary Statistics
        model.addAttribute("stats", Map.of(
                "totalEvents", allEvents.size(),
                "upcomingEvents", upcomingEvents.size(),
                "pastEvents", pastEvents.size(),
                "totalRegistrations", allRegistrations.size(),
                "pendingRegistrations", registrationStats.getOrDefault("PENDING", 0L),
                "approvedRegistrations", registrationStats.getOrDefault("APPROVED", 0L),
                "rejectedRegistrations", registrationStats.getOrDefault("REJECTED", 0L),
                "totalUsers", allUsers.size(),
                "totalStudents", userStats.getOrDefault("STUDENT", 0L),
                "totalAdmins", userStats.getOrDefault("ADMIN", 0L)
        ));

        // ✅ Latest events and registrations
        model.addAttribute("recentEvents",
                allEvents.stream()
                        .sorted(Comparator.comparing(Event::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(5)
                        .collect(Collectors.toList()));

        model.addAttribute("recentRegistrations",
                allRegistrations.stream()
                        .sorted(Comparator.comparing(Registration::getRegistrationDate, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(5)
                        .collect(Collectors.toList()));

        model.addAttribute("pendingApprovals", registrationService.getPendingRegistrations());

        return "admin/dashboard";
    }

    /** ✅ Manage events */
    @GetMapping("/events/manage")
    public String manageEvents(Model model) {
        model.addAttribute("events", eventService.getAllEvents());
        return "admin/events";
    }

    @PostMapping("/events/{eventId}/approve")
    public String approveEvent(@PathVariable Long eventId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin(session);
            eventService.approveEvent(eventId, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Event approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/events/manage";
    }

    @PostMapping("/events/{eventId}/reject")
    public String rejectEvent(@PathVariable Long eventId,
                              @RequestParam(required = false) String reason,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin(session);
            eventService.rejectEvent(eventId, reason, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Event rejected successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/events/manage";
    }

    /** ✅ Manage Users */
    @GetMapping("/users")
    public String manageUsers(Model model,
                              @RequestParam(required = false) String role,
                              @RequestParam(required = false) String query) {
        List<User> users = userService.getAllUsers();

        if (role != null && !role.isEmpty()) {
            users = users.stream().filter(u -> role.equals(u.getRole())).collect(Collectors.toList());
        }

        if (query != null && !query.isEmpty()) {
            String searchQuery = query.toLowerCase();
            users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(searchQuery)
                            || u.getFullName().toLowerCase().contains(searchQuery)
                            || u.getEmail().toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        model.addAttribute("users", users);
        model.addAttribute("selectedRole", role);
        model.addAttribute("searchQuery", query);
        return "admin/users";
    }

    @PostMapping("/users/{userId}/role")
    public String updateUserRole(@PathVariable Long userId,
                                 @RequestParam String role,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin(session);
            userService.updateUserRole(userId, role, admin);
            redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/status")
    public String updateUserStatus(@PathVariable Long userId,
                                   @RequestParam boolean active,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin(session);
            userService.updateUserStatus(userId, active, admin);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /** ✅ View Registrations */
    @GetMapping("/registrations")
    public String viewRegistrations(@RequestParam(required = false) Long eventId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String department,
                                    Model model,
                                    HttpSession session) {
        getCurrentAdmin(session);

        List<Registration> registrations = (eventId != null)
                ? registrationService.getEventRegistrations(eventService.getEventById(eventId))
                : registrationService.getAllRegistrations();

        if (status != null && !status.isEmpty()) {
            registrations = registrations.stream().filter(r -> status.equals(r.getStatus())).collect(Collectors.toList());
        }

        if (department != null && !department.isEmpty()) {
            registrations = registrations.stream()
                    .filter(r -> r.getUser() != null && department.equals(r.getUser().getDepartment()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("registrations", registrations);
        model.addAttribute("events", eventService.getAllEvents());
        model.addAttribute("selectedEvent", eventId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("departments", userService.getAllDepartments());

        return "admin/registrations";
    }

    /** ✅ Update registration status */
    @PostMapping("/registrations/{registrationId}/status")
    public String updateRegistrationStatus(@PathVariable Long registrationId,
                                           @RequestParam String status,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin(session);
            Registration registration = registrationService.updateRegistrationStatus(registrationId, status, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Status updated to " + status);
            return "redirect:/admin/registrations?eventId=" + registration.getEvent().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/registrations";
        }
    }

    /** ✅ Export registrations to CSV/XLSX */
    @GetMapping("/registrations/export")
    public ResponseEntity<Resource> exportRegistrations(@RequestParam(required = false) Long eventId,
                                                        @RequestParam(required = false, defaultValue = "csv") String format) {
        try {
            String filename = "registrations_" + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + format;

            byte[] data = registrationService.exportRegistrations(eventId, format);

            MediaType mediaType = switch (format.toLowerCase()) {
                case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                case "csv" -> MediaType.TEXT_PLAIN;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(mediaType)
                    .body(new ByteArrayResource(data));

        } catch (Exception e) {
            throw new RuntimeException("Error exporting registrations: " + e.getMessage());
        }
    }

    /** ✅ Reports */
    @GetMapping("/reports")
    public String viewReports(Model model,
                              @RequestParam(required = false) String type,
                              @RequestParam(required = false) String period) {
        LocalDateTime startDate = getStartDateForPeriod(period);

        Map<String, Object> reportData = switch (type) {
            case "events" -> generateEventReport(startDate);
            case "registrations" -> generateRegistrationReport(startDate);
            case "users" -> generateUserReport(startDate);
            default -> Map.of("error", "Invalid report type");
        };

        model.addAttribute("reportData", reportData);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedPeriod", period);
        return "admin/reports";
    }

    /** ✅ Helper methods */
    private LocalDateTime getStartDateForPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> now.minusMonths(1);
        };
    }

    private Map<String, Object> generateEventReport(LocalDateTime startDate) {
        List<Event> events = eventService.getEventsByDateRange(startDate, LocalDateTime.now());
        return Map.of(
                "totalEvents", events.size(),
                "eventsData", events,
                "eventsByDepartment", events.stream()
                        .collect(Collectors.groupingBy(e -> e.getCreatedBy().getDepartment(), Collectors.counting()))
        );
    }

    private Map<String, Object> generateRegistrationReport(LocalDateTime startDate) {
        List<Registration> registrations = registrationService.getRegistrationsByDateRange(startDate, LocalDateTime.now());
        return Map.of(
                "totalRegistrations", registrations.size(),
                "registrationsData", registrations,
                "registrationsByStatus", registrations.stream()
                        .collect(Collectors.groupingBy(Registration::getStatus, Collectors.counting()))
        );
    }

    private Map<String, Object> generateUserReport(LocalDateTime startDate) {
        List<User> users = userService.getUsersByDateRange(startDate, LocalDateTime.now());
        return Map.of(
                "totalUsers", users.size(),
                "usersData", users,
                "usersByRole", users.stream()
                        .collect(Collectors.groupingBy(User::getRole, Collectors.counting()))
        );
    }
}
