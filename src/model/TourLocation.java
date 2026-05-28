package model;

import java.time.LocalDateTime;

public class TourLocation {
    private int tourLocationId;
    private int tourId;
    private int locationId;
    private int dayNumber;
    private int sequenceOrder;
    private String description;
    private LocalDateTime createdAt;

    public TourLocation() {
    }


    public TourLocation(int tourLocationId, int tourId, int locationId, int dayNumber, int sequenceOrder, String description, LocalDateTime createdAt) {
        this.tourLocationId = tourLocationId;
        this.tourId = tourId;
        this.locationId = locationId;
        this.dayNumber = dayNumber;
        this.sequenceOrder = sequenceOrder;
        this.description = description;
        this.createdAt = createdAt;
    }


    public int getTourLocationId() {
        return tourLocationId;
    }

    public void setTourLocationId(int tourLocationId) {
        this.tourLocationId = tourLocationId;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TourLocation{tourLocationId=" + tourLocationId +
                ", tourId=" + tourId +
                ", locationId=" + locationId +
                ", dayNumber=" + dayNumber +
                ", sequenceOrder=" + sequenceOrder +
                ", description=" + description +
                ", createdAt=" + createdAt +
                '}';
    }
}
