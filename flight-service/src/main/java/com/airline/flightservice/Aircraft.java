package com.airline.flightservice;

import jakarta.persistence.*;

@Entity
@Table(name = "aircraft")
public class Aircraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String model;
    @Column(nullable = false) private int seatCapacity;
    public Long getId() { return id; }
    public String getCode() { return code; } public void setCode(String v) { code = v; }
    public String getModel() { return model; } public void setModel(String v) { model = v; }
    public int getSeatCapacity() { return seatCapacity; } public void setSeatCapacity(int v) { seatCapacity = v; }
}
