package com.example.sb.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.Registration;
import com.example.sb.demo.entity.User;
import com.example.sb.demo.repository.RegistrationRepository;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;

    public RegistrationService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    /**
     * Registers a user for a specific event.
     */
    @Transactional
    public Registration registerForEvent(Event event, User user) {
        if (event == null || user == null) {
            throw new RuntimeException("Invalid event or user information");
        }

        // Check if the user is already registered
        Optional<Registration> existing = registrationRepository.findByEventAndUser(event, user);
        if (existing.isPresent()) {
            throw new RuntimeException("You have already registered for this event");
        }

        // Check for event capacity
        long count = registrationRepository.countByEvent(event);
        if (event.getMaxParticipants() != null && count >= event.getMaxParticipants()) {
            throw new RuntimeException("Registration limit reached for this event");
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setUser(user);
        registration.setStatus("PENDING");
        registration.setRegistrationDate(LocalDateTime.now());

        return registrationRepository.save(registration);
    }

    public List<Registration> getEventRegistrations(Event event) {
        return registrationRepository.findByEvent(event);
    }

    public List<Registration> getUserRegistrations(User user) {
        return registrationRepository.findByUser(user);
    }

    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    @Transactional
    public Registration updateRegistrationStatus(Long registrationId, String status, User admin) {
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only admins can update registration status");
        }

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        registration.setStatus(status);
        return registrationRepository.save(registration);
    }

    public List<Registration> getPendingRegistrations() {
        return registrationRepository.findAll().stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Registration> getRegistrationsByDateRange(LocalDateTime start, LocalDateTime end) {
        return registrationRepository.findAll().stream()
                .filter(r -> r.getRegistrationDate() != null &&
                        (r.getRegistrationDate().isAfter(start) || r.getRegistrationDate().isEqual(start)) &&
                        (r.getRegistrationDate().isBefore(end) || r.getRegistrationDate().isEqual(end)))
                .collect(Collectors.toList());
    }

    public byte[] exportRegistrations(Long eventId, String format) {
        List<Registration> regs = (eventId == null)
                ? getAllRegistrations()
                : registrationRepository.findByEventId(eventId);

        // Simple CSV export
        StringBuilder sb = new StringBuilder();
        sb.append("Registration ID,Event ID,Event Title,User ID,Username,Email,Status,Date\n");
        for (Registration r : regs) {
            sb.append(r.getId()).append(',')
              .append(r.getEvent().getId()).append(',')
              .append(escapeCsv(r.getEvent().getTitle())).append(',')
              .append(r.getUser().getId()).append(',')
              .append(escapeCsv(r.getUser().getUsername())).append(',')
              .append(escapeCsv(r.getUser().getEmail())).append(',')
              .append(r.getStatus()).append(',')
              .append(r.getRegistrationDate()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    @Transactional
    public void cancelRegistration(Long registrationId, User user) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        if (!registration.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to cancel this registration");
        }

        registrationRepository.delete(registration);
    }
}
