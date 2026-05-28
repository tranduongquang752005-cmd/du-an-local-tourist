package model;

import java.time.LocalDateTime;

public class UserCouponUsage {
    private int userId;
    private int couponId;
    private int usageCount;
    private LocalDateTime lastUsedAt;

    public UserCouponUsage() {
    }


    public UserCouponUsage(int userId, int couponId, int usageCount, LocalDateTime lastUsedAt) {
        this.userId = userId;
        this.couponId = couponId;
        this.usageCount = usageCount;
        this.lastUsedAt = lastUsedAt;
    }


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    @Override
    public String toString() {
        return "UserCouponUsage{userId=" + userId +
                ", couponId=" + couponId +
                ", usageCount=" + usageCount +
                ", lastUsedAt=" + lastUsedAt +
                '}';
    }
}
