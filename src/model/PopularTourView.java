package model;

import java.math.BigDecimal;

public class PopularTourView {
    private int tourId;
    private String tourName;
    private String description;
    private BigDecimal basePrice;
    private int duration;
    private String theme;
    private String locationName;
    private String categoryName;
    private int totalBookings;
    private int totalPassengers;
    private int totalOccupiedSlots;
    private BigDecimal totalRevenue;
    private String imageUrl;

    public PopularTourView() {
    }


    public PopularTourView(int tourId, String tourName, String description, BigDecimal basePrice, int duration, String theme, String locationName, String categoryName, int totalBookings, int totalPassengers, int totalOccupiedSlots, BigDecimal totalRevenue, String imageUrl) {
        this.tourId = tourId;
        this.tourName = tourName;
        this.description = description;
        this.basePrice = basePrice;
        this.duration = duration;
        this.theme = theme;
        this.locationName = locationName;
        this.categoryName = categoryName;
        this.totalBookings = totalBookings;
        this.totalPassengers = totalPassengers;
        this.totalOccupiedSlots = totalOccupiedSlots;
        this.totalRevenue = totalRevenue;
        this.imageUrl = imageUrl;
    }


    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
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

    public int getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(int totalBookings) {
        this.totalBookings = totalBookings;
    }

    public int getTotalPassengers() {
        return totalPassengers;
    }

    public void setTotalPassengers(int totalPassengers) {
        this.totalPassengers = totalPassengers;
    }

    public int getTotalOccupiedSlots() {
        return totalOccupiedSlots;
    }

    public void setTotalOccupiedSlots(int totalOccupiedSlots) {
        this.totalOccupiedSlots = totalOccupiedSlots;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "PopularTourView{tourId=" + tourId +
                ", tourName=" + tourName +
                ", description=" + description +
                ", basePrice=" + basePrice +
                ", duration=" + duration +
                ", theme=" + theme +
                ", locationName=" + locationName +
                ", categoryName=" + categoryName +
                ", totalBookings=" + totalBookings +
                ", totalPassengers=" + totalPassengers +
                ", totalOccupiedSlots=" + totalOccupiedSlots +
                ", totalRevenue=" + totalRevenue +
                ", imageUrl=" + imageUrl +
                '}';
    }
}
