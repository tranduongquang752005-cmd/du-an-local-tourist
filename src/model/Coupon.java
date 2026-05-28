package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Coupon {
    private int couponId;
    private String couponCode;
    private String discountType;
    private BigDecimal discountValue;
    private int maxUsagePerUser;
    private Integer maxTotalUsage;
    private int currentTotalUsage;
    private BigDecimal maxDiscountAmount;
    private LocalDate expiryDate;
    private boolean active;
    private Integer createdBy;
    private LocalDateTime createdAt;

    public Coupon() {
    }

    public Coupon(int couponId,
                  String couponCode,
                  String discountType,
                  BigDecimal discountValue,
                  int maxUsagePerUser,
                  Integer maxTotalUsage,
                  int currentTotalUsage,
                  BigDecimal maxDiscountAmount,
                  LocalDate expiryDate,
                  boolean active,
                  Integer createdBy,
                  LocalDateTime createdAt) {
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxUsagePerUser = maxUsagePerUser;
        this.maxTotalUsage = maxTotalUsage;
        this.currentTotalUsage = currentTotalUsage;
        this.maxDiscountAmount = maxDiscountAmount;
        this.expiryDate = expiryDate;
        this.active = active;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public int getMaxUsagePerUser() {
        return maxUsagePerUser;
    }

    public void setMaxUsagePerUser(int maxUsagePerUser) {
        this.maxUsagePerUser = maxUsagePerUser;
    }

    public Integer getMaxTotalUsage() {
        return maxTotalUsage;
    }

    public void setMaxTotalUsage(Integer maxTotalUsage) {
        this.maxTotalUsage = maxTotalUsage;
    }

    public int getCurrentTotalUsage() {
        return currentTotalUsage;
    }

    public void setCurrentTotalUsage(int currentTotalUsage) {
        this.currentTotalUsage = currentTotalUsage;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
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

    @Override
    public String toString() {
        return "Coupon{" +
                "couponId=" + couponId +
                ", couponCode='" + couponCode + '\'' +
                ", discountType='" + discountType + '\'' +
                ", discountValue=" + discountValue +
                ", maxUsagePerUser=" + maxUsagePerUser +
                ", maxTotalUsage=" + maxTotalUsage +
                ", currentTotalUsage=" + currentTotalUsage +
                ", maxDiscountAmount=" + maxDiscountAmount +
                ", expiryDate=" + expiryDate +
                ", active=" + active +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                '}';
    }
}