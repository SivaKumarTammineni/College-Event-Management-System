package com.example.sb.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.User;
import com.example.sb.demo.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
	  private final EventRepository eventRepository;

	    // ✅ Constructor Injection (preferred)
	   
	    public EventService(EventRepository eventRepository) {
	        this.eventRepository = eventRepository;
	    }

    public List<Event> getAllEvents() {
        return eventRepository.findAllByOrderByEventDateDesc();
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));
    }

    public List<Event> getUpcomingEvents() {
        return eventRepository.findByEventDateAfterOrderByEventDateAsc(LocalDateTime.now());
    }

    public List<Event> getUserEvents(User user) {
        return eventRepository.findByCreatedBy(user);
    }

    public List<Event> getPastEvents() {
        return eventRepository.findByEventDateBeforeOrderByEventDateDesc(LocalDateTime.now());
    }

    public List<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findAll().stream()
                .filter(e -> e.getEventDate() != null &&
                        (e.getEventDate().isEqual(start) || e.getEventDate().isAfter(start)) &&
                        (e.getEventDate().isEqual(end) || e.getEventDate().isBefore(end)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Event approveEvent(Long eventId, User admin) {
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only admins can approve events");
        }
        Event event = getEventById(eventId);
        event.setStatus("APPROVED");
        return eventRepository.save(event);
    }

    @Transactional
    public Event rejectEvent(Long eventId, String reason, User admin) {
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only admins can reject events");
        }
        Event event = getEventById(eventId);
        event.setStatus("REJECTED");
        // reason is currently not persisted (no field). Could be logged or stored in future.
        return eventRepository.save(event);
    }

//    @Transactional
//    public Event createEvent(Event event, User user) {
//        event.setCreatedBy(user);
//        return eventRepository.save(event);
//    }

    @Transactional
    public Event updateEvent(Long id, Event eventDetails, User user) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!isAuthorizedToModify(event, user)) {
            throw new RuntimeException("Not authorized to modify this event");
        }

        event.setTitle(eventDetails.getTitle());
        event.setDescription(eventDetails.getDescription());
        event.setEventDate(eventDetails.getEventDate());
        event.setVenue(eventDetails.getVenue());
        event.setImageUrl(eventDetails.getImageUrl());
        event.setMaxParticipants(eventDetails.getMaxParticipants());

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long id, User user) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!isAuthorizedToModify(event, user)) {
            throw new RuntimeException("Not authorized to delete this event");
        }

        eventRepository.delete(event);
    }

    private boolean isAuthorizedToModify(Event event, User user) {
        return user.getRole().equals("ADMIN") || event.getCreatedBy().getId().equals(user.getId());
    }
    
    @Transactional
    public Event createEvent(Event event, User creator) {
        event.setCreatedBy(creator); // ✅ Important for ownership
        event.setCreatedAt(LocalDateTime.now());
        return eventRepository.save(event);
    }

}