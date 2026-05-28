package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TourCancellation {
    private int tourCancelId;
    private int scheduleId;
    private String cancelReason;
    private String resolutionType;
    private Integer newScheduleId;
    private BigDecimal refundPercent;
    private LocalDateTime createdAt;

    public TourCancellation() {
    }


    public TourCancellation(int tourCancelId, int scheduleId, String cancelReason, String resolutionType, Integer newScheduleId, BigDecimal refundPercent, LocalDateTime createdAt) {
        this.tourCancelId = tourCancelId;
        this.scheduleId = scheduleId;
        this.cancelReason = cancelReason;
        this.resolutionType = resolutionType;
        this.newScheduleId = newScheduleId;
        this.refundPercent = refundPercent;
        this.createdAt = createdAt;
    }


    public int getTourCancelId() {
        return tourCancelId;
    }

    public void setTourCancelId(int tourCancelId) {
        this.tourCancelId = tourCancelId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getResolutionType() {
        return resolutionType;
    }

    public void setResolutionType(String resolutionType) {
        this.resolutionType = resolutionType;
    }

    public Integer getNewScheduleId() {
        return newScheduleId;
    }

    public void setNewScheduleId(Integer newScheduleId) {
        this.newScheduleId = newScheduleId;
    }

    public BigDecimal getRefundPercent() {
        return refundPercent;
    }

    public void setRefundPercent(BigDecimal refundPercent) {
        this.refundPercent = refundPercent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TourCancellation{tourCancelId=" + tourCancelId +
                ", scheduleId=" + scheduleId +
                ", cancelReason=" + cancelReason +
                ", resolutionType=" + resolutionType +
                ", newScheduleId=" + newScheduleId +
                ", refundPercent=" + refundPercent +
                ", createdAt=" + createdAt +
                '}';
    }
}
