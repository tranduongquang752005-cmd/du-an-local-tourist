package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Booking {
    public enum Status {
        PENDING,
        PAID,
        COMPLETED,
        CANCELLED
    }

    private int bookingId;
    private int userId;
    private int tourId;
    private int scheduleId;
    private LocalDateTime bookingDate;
    private BigDecimal totalPrice;
    private Integer couponId;
    private BigDecimal discountAmount;
    private BigDecimal surchargeAmount;
    private BigDecimal finalPrice;
    private Status status;
    private LocalDateTime createdAt;

    private String tourName;
    private String userFullName;

    public Booking() {
    }

    public Booking(int bookingId, int userId, int tourId, int scheduleId,
                   LocalDateTime bookingDate, BigDecimal totalPrice, Integer couponId,
                   BigDecimal discountAmount, BigDecimal surchargeAmount, BigDecimal finalPrice,
                   Status status, LocalDateTime createdAt) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.tourId = tourId;
        this.scheduleId = scheduleId;
        this.bookingDate = bookingDate;
        this.totalPrice = totalPrice;
        this.couponId = couponId;
        this.discountAmount = discountAmount;
        this.surchargeAmount = surchargeAmount;
        this.finalPrice = finalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getCouponId() {
        return couponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getSurchargeAmount() {
        return surchargeAmount;
    }

    public void setSurchargeAmount(BigDecimal surchargeAmount) {
        this.surchargeAmount = surchargeAmount;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    // Overload để code cũ setStatus(String) vẫn không lỗi
    public void setStatus(String status) {
        if (status == null) {
            this.status = null;
            return;
        }

        try {
            this.status = Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            this.status = null;
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTourName() {
        return tourName;
    }

    public void setTourName(String tourName) {
        this.tourName = tourName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", userId=" + userId +
                ", tourId=" + tourId +
                ", scheduleId=" + scheduleId +
                ", tourName='" + tourName + '\'' +
                ", userFullName='" + userFullName + '\'' +
                ", totalPrice=" + totalPrice +
                ", discountAmount=" + discountAmount +
                ", surchargeAmount=" + surchargeAmount +
                ", finalPrice=" + finalPrice +
                ", status=" + status +
                ", bookingDate=" + bookingDate +
                '}';
    }
}