package com.example.sb.demo.controller;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.User;
import com.example.sb.demo.service.EventService;
import com.example.sb.demo.service.RegistrationService;
import com.example.sb.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class EventController {

    private final EventService eventService;
    private final UserService userService;
    private final RegistrationService registrationService;

    // ✅ Constructor-based dependency injection for all required services
    @Autowired
    public EventController(EventService eventService,
                           UserService userService,
                           RegistrationService registrationService) {
        this.eventService = eventService;
        this.userService = userService;
        this.registrationService = registrationService;
    }

    // ✅ Utility to get currently logged-in user
    private User getCurrentUser(HttpSession session) {
        return userService.getCurrentUser(session)
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        userService.getCurrentUser(session).ifPresent(user -> {
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", userService.isAdmin(user));
        });
        model.addAttribute("upcomingEvents", eventService.getUpcomingEvents());
        return "home";
    }

    @GetMapping("/events")
    public String listEvents(Model model, HttpSession session) {
        User user = getCurrentUser(session);
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", userService.isAdmin(user));
        model.addAttribute("events", eventService.getAllEvents());
        return "events/list";
    }

    @GetMapping("/events/new")
    public String newEventForm(Model model, HttpSession session) {
        getCurrentUser(session);
        model.addAttribute("event", new Event());
        return "events/form";
    }

    @PostMapping("/events")
    public String createEvent(@RequestParam String eventDate,
                              @RequestParam String eventTime,
                              @RequestParam String title,
                              @RequestParam String description,
                              @RequestParam String venue,
                              @RequestParam(required = false) Integer maxParticipants,
                              @RequestParam(required = false) MultipartFile imageFile,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime eventDateTime = combineDateTime(eventDate, eventTime);
            if (eventDateTime.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Event date must be in the future");
            }

            Event event = new Event();
            event.setTitle(title);
            event.setDescription(description);
            event.setVenue(venue);
            event.setMaxParticipants(maxParticipants);
            event.setEventDate(eventDateTime);

            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = handleImageUpload(imageFile);
                event.setImageUrl(imageUrl);
            }

            User user = getCurrentUser(session);
            eventService.createEvent(event, user);
            redirectAttributes.addFlashAttribute("successMessage", "Event created successfully!");
            return "redirect:/events";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/new";
        }
    }

    private LocalDateTime combineDateTime(String date, String timeStr) {
        try {
            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime dateTime = LocalDateTime.parse(date + "T00:00:00");
            return LocalDateTime.of(dateTime.toLocalDate(), time);
        } catch (Exception e) {
            throw new RuntimeException("Invalid date/time format. Use YYYY-MM-DD and HH:mm.");
        }
    }

    private String handleImageUpload(MultipartFile file) throws IOException {
        if (file.isEmpty()) return null;
        String uploadDir = "uploads/events";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/" + uploadDir + "/" + fileName;
    }

    @GetMapping("/events/{id}/edit")
    public String editEventForm(@PathVariable Long id, Model model, HttpSession session) {
        User user = getCurrentUser(session);
        Event event = eventService.getEventById(id);

        if (!user.getRole().equals("ADMIN") && !event.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to edit this event");
        }

        model.addAttribute("event", event);
        return "events/form";
    }

    @PostMapping("/events/{id}")
    public String updateEvent(@PathVariable Long id, @ModelAttribute Event event,
                              HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(session);
            eventService.updateEvent(id, event, user);
            redirectAttributes.addFlashAttribute("successMessage", "Event updated successfully!");
            return "redirect:/events";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + id + "/edit";
        }
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(session);
            eventService.deleteEvent(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/events";
    }

    @PostMapping("/events/{id}/register")
    public String registerForEvent(@PathVariable Long id, HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(session);
            Event event = eventService.getEventById(id);
            registrationService.registerForEvent(event, user);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully registered for the event!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/events";
    }
}
