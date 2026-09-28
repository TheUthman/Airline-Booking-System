package com.airline.flightservice;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {}
