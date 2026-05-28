package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TourSchedule {
    private int scheduleId;
    private int tourId;
    private LocalDate scheduleDate;
    private int availableSlots;
    private int bookedSlots;
    private BigDecimal priceMultiplier;
    private BigDecimal surcharge;
    private Integer fuelPriceId;
    private LocalDateTime createdAt;

    public TourSchedule() {
    }


    public TourSchedule(int scheduleId, int tourId, LocalDate scheduleDate, int availableSlots, int bookedSlots, BigDecimal priceMultiplier, BigDecimal surcharge, Integer fuelPriceId, LocalDateTime createdAt) {
        this.scheduleId = scheduleId;
        this.tourId = tourId;
        this.scheduleDate = scheduleDate;
        this.availableSlots = availableSlots;
        this.bookedSlots = bookedSlots;
        this.priceMultiplier = priceMultiplier;
        this.surcharge = surcharge;
        this.fuelPriceId = fuelPriceId;
        this.createdAt = createdAt;
    }


    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public int getBookedSlots() {
        return bookedSlots;
    }

    public void setBookedSlots(int bookedSlots) {
        this.bookedSlots = bookedSlots;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(BigDecimal surcharge) {
        this.surcharge = surcharge;
    }

    public Integer getFuelPriceId() {
        return fuelPriceId;
    }

    public void setFuelPriceId(Integer fuelPriceId) {
        this.fuelPriceId = fuelPriceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getRemainingSlots() {
        return availableSlots - bookedSlots;
    }

    @Override
    public String toString() {
        return "TourSchedule{scheduleId=" + scheduleId +
                ", tourId=" + tourId +
                ", scheduleDate=" + scheduleDate +
                ", availableSlots=" + availableSlots +
                ", bookedSlots=" + bookedSlots +
                ", priceMultiplier=" + priceMultiplier +
                ", surcharge=" + surcharge +
                ", fuelPriceId=" + fuelPriceId +
                ", createdAt=" + createdAt +
                '}';
    }
}
