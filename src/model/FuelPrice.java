package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FuelPrice {
    private int fuelPriceId;
    private BigDecimal price;
    private LocalDate effectiveDate;
    private LocalDateTime createdAt;

    public FuelPrice() {
    }


    public FuelPrice(int fuelPriceId, BigDecimal price, LocalDate effectiveDate, LocalDateTime createdAt) {
        this.fuelPriceId = fuelPriceId;
        this.price = price;
        this.effectiveDate = effectiveDate;
        this.createdAt = createdAt;
    }


    public int getFuelPriceId() {
        return fuelPriceId;
    }

    public void setFuelPriceId(int fuelPriceId) {
        this.fuelPriceId = fuelPriceId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FuelPrice{fuelPriceId=" + fuelPriceId +
                ", price=" + price +
                ", effectiveDate=" + effectiveDate +
                ", createdAt=" + createdAt +
                '}';
    }
}
