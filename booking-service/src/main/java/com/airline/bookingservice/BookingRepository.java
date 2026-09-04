package com.airline.bookingservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);

    Optional<Booking> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
