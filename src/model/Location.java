package model;

import java.time.LocalDateTime;

public class Location {
    private int locationId;
    private String locationName;
    private String descriptionLocation;
    private LocalDateTime createdAt;

    public Location() {
    }


    public Location(int locationId, String locationName, String descriptionLocation, LocalDateTime createdAt) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.descriptionLocation = descriptionLocation;
        this.createdAt = createdAt;
    }


    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getDescriptionLocation() {
        return descriptionLocation;
    }

    public void setDescriptionLocation(String descriptionLocation) {
        this.descriptionLocation = descriptionLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Location{locationId=" + locationId +
                ", locationName=" + locationName +
                ", descriptionLocation=" + descriptionLocation +
                ", createdAt=" + createdAt +
                '}';
    }
}
