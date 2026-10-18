package com.airline.flightservice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByCodeIgnoreCase(String code);
}
