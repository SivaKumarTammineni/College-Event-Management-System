package com.example.sb.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.Registration;
import com.example.sb.demo.entity.User;
import com.example.sb.demo.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationRepository registrationRepository;
    @Autowired
    public RegistrationService(RegistrationRepository registrationRepository) {
    	this.registrationRepository=registrationRepository;
    }
    @Transactional
    public Registration registerForEvent(Event event, User user) {
        if (registrationRepository.existsByEventAndUser(event, user)) {
            throw new RuntimeException("Already registered for this event");
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setUser(user);
        registration.setStatus("PENDING");

        return registrationRepository.save(registration);
    }

    public List<Registration> getEventRegistrations(Event event) {
        return registrationRepository.findByEvent(event);
    }

    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    public List<Registration> getUserRegistrations(User user) {
        return registrationRepository.findByUser(user);
    }

    @Transactional
    public Registration updateRegistrationStatus(Long registrationId, String status, User admin) {
        if (!admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("Only admins can update registration status");
        }

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        registration.setStatus(status);
        return registrationRepository.save(registration);
    }

    @Transactional
    public Registration updateRegistrationStatus(Long registrationId, String status, String comment, User admin) {
        // comment is accepted but not persisted (Registration has no comment field)
        return updateRegistrationStatus(registrationId, status, admin);
    }

    public List<Registration> getPendingRegistrations() {
        return registrationRepository.findAll().stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Registration> getRegistrationsByDateRange(LocalDateTime start, LocalDateTime end) {
        return registrationRepository.findAll().stream()
                .filter(r -> r.getRegistrationDate() != null &&
                        (r.getRegistrationDate().isEqual(start) || r.getRegistrationDate().isAfter(start)) &&
                        (r.getRegistrationDate().isEqual(end) || r.getRegistrationDate().isBefore(end)))
                .collect(Collectors.toList());
    }

    public byte[] exportRegistrations(Long eventId, String format) {
        List<Registration> regs = (eventId == null)
                ? getAllRegistrations()
                : registrationRepository.findByEvent(getEventById(eventId));

        // Simple CSV export for now
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

    // helper to resolve event when exporting by id
    private Event getEventById(Long eventId) {
        return registrationRepository.findAll().stream()
                .map(Registration::getEvent)
                .filter(e -> e != null && e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
    }

    @Transactional
    public void cancelRegistration(Long registrationId, User user) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        if (!registration.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to cancel this registration");
        }

        registrationRepository.delete(registration);
    }
}