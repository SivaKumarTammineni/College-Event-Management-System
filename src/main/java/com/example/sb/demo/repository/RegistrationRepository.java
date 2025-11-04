package com.example.sb.demo.repository;

import com.example.sb.demo.entity.Event;
import com.example.sb.demo.entity.Registration;
import com.example.sb.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByUser(User user);
    List<Registration> findByEvent(Event event);
    Optional<Registration> findByEventAndUser(Event event, User user);
    boolean existsByEventAndUser(Event event, User user);
    List<Registration> findByEventAndStatus(Event event, String status);
    List<Registration> findByUserAndStatus(User user, String status);
}