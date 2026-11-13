package com.example.air.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 측정소 */
@Entity
@Table(name = "station")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(length = 200)
    private String address;

    private Double latitude;
    private Double longitude;
    private boolean active = true;
    @Column(name = "airkorea_name", length = 50)
    private String airkoreaName;
    @Column(name = "kma_stn_id")
    private Integer kmaStnId;
    private LocalDateTime createdAt;

    public String getAirkoreaName() { return airkoreaName; }
    public void setAirkoreaName(String airkoreaName) { this.airkoreaName = airkoreaName; }
    public Integer getKmaStnId() { return kmaStnId; }
    public void setKmaStnId(Integer kmaStnId) { this.kmaStnId = kmaStnId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
