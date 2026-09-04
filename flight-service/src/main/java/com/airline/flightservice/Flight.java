package com.airline.flightservice;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "flights",
        indexes =
                @Index(name = "idx_flight_search", columnList = "origin,destination,departureTime"))
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String flightNumber;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fare;

    @Column(nullable = false)
    private int availableSeats;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String v) {
        flightNumber = v;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String v) {
        origin = v;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String v) {
        destination = v;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime v) {
        departureTime = v;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime v) {
        arrivalTime = v;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal v) {
        fare = v;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int v) {
        availableSeats = v;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        active = v;
    }
}
