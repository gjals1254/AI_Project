package com.example.air.dto;

import com.example.air.domain.Station;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StationForm {

    @NotBlank(message = "측정소명을 입력하세요.") @Size(max = 50)
    private String name;

    @NotBlank(message = "시도를 입력하세요.") @Size(max = 50)
    private String region;

    @Size(max = 200)
    private String address;

    private Double latitude;
    private Double longitude;
    private boolean active = true;

    @Size(max = 50)
    private String airkoreaName;

    @Min(1) @Max(999)
    private Integer kmaStnId;

    public static StationForm from(Station s) {
        StationForm f = new StationForm();
        f.name = s.getName();
        f.region = s.getRegion();
        f.address = s.getAddress();
        f.latitude = s.getLatitude();
        f.longitude = s.getLongitude();
        f.active = s.isActive();
        f.airkoreaName = s.getAirkoreaName();
        f.kmaStnId = s.getKmaStnId();
        return f;
    }

    public void applyTo(Station s) {
        s.setName(name.trim());
        s.setRegion(region.trim());
        s.setAddress(address);
        s.setLatitude(latitude);
        s.setLongitude(longitude);
        s.setActive(active);
        s.setAirkoreaName(airkoreaName == null || airkoreaName.isBlank() ? null : airkoreaName.trim());
        s.setKmaStnId(kmaStnId);
    }

    public String getAirkoreaName() { return airkoreaName; }
    public void setAirkoreaName(String airkoreaName) { this.airkoreaName = airkoreaName; }
    public Integer getKmaStnId() { return kmaStnId; }
    public void setKmaStnId(Integer kmaStnId) { this.kmaStnId = kmaStnId; }

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
}
