package model;

import java.math.BigDecimal;

public class FeaturedTourView {
    private int featuredTourId;
    private int tourId;
    private String tourName;
    private String description;
    private BigDecimal basePrice;
    private int duration;
    private String theme;
    private String locationName;
    private String categoryName;
    private int displayOrder;
    private String featuredTitle;
    private String featuredDescription;
    private String imageUrl;

    public FeaturedTourView() {
    }


    public FeaturedTourView(int featuredTourId, int tourId, String tourName, String description, BigDecimal basePrice, int duration, String theme, String locationName, String categoryName, int displayOrder, String featuredTitle, String featuredDescription, String imageUrl) {
        this.featuredTourId = featuredTourId;
        this.tourId = tourId;
        this.tourName = tourName;
        this.description = description;
        this.basePrice = basePrice;
        this.duration = duration;
        this.theme = theme;
        this.locationName = locationName;
        this.categoryName = categoryName;
        this.displayOrder = displayOrder;
        this.featuredTitle = featuredTitle;
        this.featuredDescription = featuredDescription;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "FeaturedTourView{featuredTourId=" + featuredTourId +
                ", tourId=" + tourId +
                ", tourName=" + tourName +
                ", description=" + description +
                ", basePrice=" + basePrice +
                ", duration=" + duration +
                ", theme=" + theme +
                ", locationName=" + locationName +
                ", categoryName=" + categoryName +
                ", displayOrder=" + displayOrder +
                ", featuredTitle=" + featuredTitle +
                ", featuredDescription=" + featuredDescription +
                ", imageUrl=" + imageUrl +
                '}';
    }
}
