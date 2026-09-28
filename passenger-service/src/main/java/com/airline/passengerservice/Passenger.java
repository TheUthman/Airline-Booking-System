package com.airline.passengerservice;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "passengers",
        indexes = @Index(name = "idx_passenger_owner", columnList = "ownerEmail"))
public class Passenger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    private String phone;
    private String documentNumber;
    private String passportNationality;
    private LocalDate passportExpiryDate;
    @Column(nullable = false)
    private int frequentFlyerPoints = 0;
    @Column(nullable = false)
    private boolean savedTraveler = true;

    public Long getId() {
        return id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String v) {
        ownerEmail = v;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String v) {
        firstName = v;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String v) {
        lastName = v;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate v) {
        dateOfBirth = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        phone = v;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String v) {
        documentNumber = v;
    }
    public String getPassportNationality() { return passportNationality; }
    public void setPassportNationality(String v) { passportNationality = v; }
    public LocalDate getPassportExpiryDate() { return passportExpiryDate; }
    public void setPassportExpiryDate(LocalDate v) { passportExpiryDate = v; }
    public int getFrequentFlyerPoints() { return frequentFlyerPoints; }
    public void setFrequentFlyerPoints(int v) { frequentFlyerPoints = v; }
    public boolean isSavedTraveler() { return savedTraveler; }
    public void setSavedTraveler(boolean v) { savedTraveler = v; }
}
