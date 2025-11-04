package com.example.sb.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.User;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCreatedBy(User user);
    List<Event> findByEventDateAfterOrderByEventDateAsc(LocalDateTime date);
    List<Event> findByEventDateBeforeOrderByEventDateDesc(LocalDateTime date);
    List<Event> findAllByOrderByEventDateDesc();
}
