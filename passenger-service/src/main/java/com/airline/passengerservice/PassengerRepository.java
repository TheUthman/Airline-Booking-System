package com.airline.passengerservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    List<Passenger> findByOwnerEmail(String ownerEmail);

    Optional<Passenger> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
