package model;

import java.time.LocalDateTime;

public class TourImage {
    private int imageId;
    private int tourId;
    private String imageUrl;
    private boolean active;
    private LocalDateTime createdAt;

    public TourImage() {
    }


    public TourImage(int imageId, int tourId, String imageUrl, boolean active, LocalDateTime createdAt) {
        this.imageId = imageId;
        this.tourId = tourId;
        this.imageUrl = imageUrl;
        this.active = active;
        this.createdAt = createdAt;
    }


    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    @Override
    public String toString() {
        return "TourImage{imageId=" + imageId +
                ", tourId=" + tourId +
                ", imageUrl=" + imageUrl +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
