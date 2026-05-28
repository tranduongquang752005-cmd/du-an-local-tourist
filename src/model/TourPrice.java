package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TourPrice {
    private int priceId;
    private int tourId;
    private LocalDate effectiveDate;
    private BigDecimal price;
    private String reason;
    private LocalDateTime createdAt;

    public TourPrice() {
    }


    public TourPrice(int priceId, int tourId, LocalDate effectiveDate, BigDecimal price, String reason, LocalDateTime createdAt) {
        this.priceId = priceId;
        this.tourId = tourId;
        this.effectiveDate = effectiveDate;
        this.price = price;
        this.reason = reason;
        this.createdAt = createdAt;
    }


    public int getPriceId() {
        return priceId;
    }

    public void setPriceId(int priceId) {
        this.priceId = priceId;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TourPrice{priceId=" + priceId +
                ", tourId=" + tourId +
                ", effectiveDate=" + effectiveDate +
                ", price=" + price +
                ", reason=" + reason +
                ", createdAt=" + createdAt +
                '}';
    }
}
