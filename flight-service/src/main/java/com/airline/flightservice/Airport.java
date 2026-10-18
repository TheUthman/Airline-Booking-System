package com.airline.flightservice;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "airports")
public class Airport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String country;

    // Decimal degrees. Required for route-distance fare estimates.
    private Double latitude;
    private Double longitude;

    // Configurable estimated airport/handling charge for one flight movement.
    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedAirportCharge = BigDecimal.ZERO;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getCity() { return city; }
    public void setCity(String v) { city = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { country = v; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double v) { latitude = v; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double v) { longitude = v; }
    public BigDecimal getEstimatedAirportCharge() {
        return estimatedAirportCharge == null ? BigDecimal.ZERO : estimatedAirportCharge;
    }
    public void setEstimatedAirportCharge(BigDecimal v) {
        estimatedAirportCharge = v == null ? BigDecimal.ZERO : v;
    }
}
