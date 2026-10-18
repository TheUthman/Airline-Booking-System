package com.airline.flightservice;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "aircraft")
public class Aircraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private int seatCapacity;

    // Estimated specifications used by the cost-based fare calculator.
    // These are estimates, not live aircraft telemetry.
    @Column(precision = 12, scale = 3)
    private BigDecimal fuelConsumptionLitresPerHour;

    @Column(precision = 8, scale = 2)
    private BigDecimal cruiseSpeedKmh;

    @Column(precision = 12, scale = 2)
    private BigDecimal operatingCostPerHour;

    @Column(precision = 12, scale = 2)
    private BigDecimal maintenanceCostPerHour;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getModel() { return model; }
    public void setModel(String v) { model = v; }
    public int getSeatCapacity() { return seatCapacity; }
    public void setSeatCapacity(int v) { seatCapacity = v; }
    public BigDecimal getFuelConsumptionLitresPerHour() { return fuelConsumptionLitresPerHour; }
    public void setFuelConsumptionLitresPerHour(BigDecimal v) { fuelConsumptionLitresPerHour = v; }
    public BigDecimal getCruiseSpeedKmh() { return cruiseSpeedKmh; }
    public void setCruiseSpeedKmh(BigDecimal v) { cruiseSpeedKmh = v; }
    public BigDecimal getOperatingCostPerHour() { return operatingCostPerHour; }
    public void setOperatingCostPerHour(BigDecimal v) { operatingCostPerHour = v; }
    public BigDecimal getMaintenanceCostPerHour() { return maintenanceCostPerHour; }
    public void setMaintenanceCostPerHour(BigDecimal v) { maintenanceCostPerHour = v; }
}
