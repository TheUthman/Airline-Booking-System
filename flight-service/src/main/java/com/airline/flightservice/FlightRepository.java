package com.airline.flightservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    /**
     * Finds active flights for the given route and date window, with optional server-side
     * filters for airline, max fare, and max duration in minutes.
     * Null filter values mean "no restriction".
     */
    @Query("""
            SELECT f FROM Flight f
            WHERE f.origin       = UPPER(:origin)
              AND f.destination  = UPPER(:destination)
              AND f.departureTime >= :start
              AND f.departureTime <  :end
              AND f.active       = true
              AND f.availableSeats >= :passengers
              AND (UPPER(:cabin) <> 'BUSINESS' OR f.businessFare > 0)
              AND (:airline IS NULL OR UPPER(f.airline) = UPPER(:airline))
              AND (:maxPrice IS NULL OR
                   CASE WHEN UPPER(:cabin) = 'BUSINESS' THEN f.businessFare ELSE f.fare END <= :maxPrice)
              AND (:maxDurationMinutes IS NULL
                   OR (FUNCTION('TIMESTAMPDIFF', MINUTE, f.departureTime, f.arrivalTime)) <= :maxDurationMinutes)
            ORDER BY f.departureTime
            """)
    List<Flight> search(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("passengers") int passengers,
            @Param("cabin") String cabin,
            @Param("airline") String airline,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("maxDurationMinutes") Integer maxDurationMinutes);
}
