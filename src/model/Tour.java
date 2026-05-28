package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Tour {
    private int tourId;
    private int locationId;
    private Integer categoryId;
    private String tourName;
    private String description;
    private BigDecimal basePrice;
    private int duration;
    private String theme;
    private boolean active;
    private LocalDateTime createdAt;
    private String locationName;
    private String categoryName;
    private String imageUrl;

    public Tour() {
    }


    public Tour(int tourId, int locationId, Integer categoryId, String tourName, String description, BigDecimal basePrice, int duration, String theme, boolean active, LocalDateTime createdAt, String locationName, String categoryName, String imageUrl) {
        this.tourId = tourId;
        this.locationId = locationId;
        this.categoryId = categoryId;
        this.tourName = tourName;
        this.description = description;
        this.basePrice = basePrice;
        this.duration = duration;
        this.theme = theme;
        this.active = active;
        this.createdAt = createdAt;
        this.locationName = locationName;
        this.categoryName = categoryName;
        this.imageUrl = imageUrl;
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getTourName() {
        return tourName;
    }

    public void setTourName(String tourName) {
        this.tourName = tourName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "Tour{tourId=" + tourId +
                ", locationId=" + locationId +
                ", categoryId=" + categoryId +
                ", tourName=" + tourName +
                ", description=" + description +
                ", basePrice=" + basePrice +
                ", duration=" + duration +
                ", theme=" + theme +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", locationName=" + locationName +
                ", categoryName=" + categoryName +
                ", imageUrl=" + imageUrl +
                '}';
    }
}
