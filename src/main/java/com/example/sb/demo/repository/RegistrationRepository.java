package com.example.sb.demo.repository;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.Registration;
import com.example.sb.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByEventAndUser(Event event, User user);
    List<Registration> findByEvent(Event event);
    List<Registration> findByUser(User user);
    List<Registration> findByEventId(Long eventId);
    long countByEvent(Event event);
    boolean existsByEventAndUser(Event event, User user);
}
