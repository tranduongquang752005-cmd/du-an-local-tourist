package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AddOn {
    private int addOnId;
    private String addOnName;
    private BigDecimal price;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    public AddOn() {
    }


    public AddOn(int addOnId, String addOnName, BigDecimal price, String description, boolean active, LocalDateTime createdAt) {
        this.addOnId = addOnId;
        this.addOnName = addOnName;
        this.price = price;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
    }


    public int getAddOnId() {
        return addOnId;
    }

    public void setAddOnId(int addOnId) {
        this.addOnId = addOnId;
    }

    public String getAddOnName() {
        return addOnName;
    }

    public void setAddOnName(String addOnName) {
        this.addOnName = addOnName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        return "AddOn{addOnId=" + addOnId +
                ", addOnName=" + addOnName +
                ", price=" + price +
                ", description=" + description +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
