package com.airline.flightservice;

import jakarta.persistence.*;

@Entity
@Table(name = "airports")
public class Airport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 3) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String city;
    @Column(nullable = false) private String country;
    public Long getId() { return id; }
    public String getCode() { return code; } public void setCode(String v) { code = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getCity() { return city; } public void setCity(String v) { city = v; }
    public String getCountry() { return country; } public void setCountry(String v) { country = v; }
}
