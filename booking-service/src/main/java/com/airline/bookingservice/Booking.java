package com.airline.bookingservice;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "bookings", indexes = @Index(name = "idx_booking_owner", columnList = "ownerEmail"))
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String pnr;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private Long flightId;

    @Column(nullable = false)
    private Long passengerId;

    @Column(nullable = false)
    private String seatNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String v) {
        pnr = v;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String v) {
        ownerEmail = v;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long v) {
        flightId = v;
    }

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long v) {
        passengerId = v;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String v) {
        seatNumber = v;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal v) {
        amount = v;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
