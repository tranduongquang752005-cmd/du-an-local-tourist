package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FeaturedTour {
    private int featuredTourId;
    private int tourId;
    private int displayOrder;
    private String featuredTitle;
    private String featuredDescription;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FeaturedTour() {
    }


    public FeaturedTour(int featuredTourId, int tourId, int displayOrder, String featuredTitle, String featuredDescription, LocalDate startDate, LocalDate endDate, boolean active, Integer createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.featuredTourId = featuredTourId;
        this.tourId = tourId;
        this.displayOrder = displayOrder;
        this.featuredTitle = featuredTitle;
        this.featuredDescription = featuredDescription;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public int getFeaturedTourId() {
        return featuredTourId;
    }

    public void setFeaturedTourId(int featuredTourId) {
        this.featuredTourId = featuredTourId;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getFeaturedTitle() {
        return featuredTitle;
    }

    public void setFeaturedTitle(String featuredTitle) {
        this.featuredTitle = featuredTitle;
    }

    public String getFeaturedDescription() {
        return featuredDescription;
    }

    public void setFeaturedDescription(String featuredDescription) {
        this.featuredDescription = featuredDescription;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "FeaturedTour{featuredTourId=" + featuredTourId +
                ", tourId=" + tourId +
                ", displayOrder=" + displayOrder +
                ", featuredTitle=" + featuredTitle +
                ", featuredDescription=" + featuredDescription +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", active=" + active +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
