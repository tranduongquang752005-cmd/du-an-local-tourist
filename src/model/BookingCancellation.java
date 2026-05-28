package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingCancellation {
    private int bookingCancelId;
    private int bookingId;
    private String cancelBy;
    private String cancelReason;
    private BigDecimal refundPercent;
    private BigDecimal refundAmount;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    public BookingCancellation() {
    }

    public BookingCancellation(int bookingCancelId,
                               int bookingId,
                               String cancelBy,
                               String cancelReason,
                               BigDecimal refundPercent,
                               BigDecimal refundAmount,
                               LocalDateTime cancelledAt,
                               LocalDateTime createdAt) {
        this.bookingCancelId = bookingCancelId;
        this.bookingId = bookingId;
        this.cancelBy = cancelBy;
        this.cancelReason = cancelReason;
        this.refundPercent = refundPercent;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
    }

    public int getBookingCancelId() {
        return bookingCancelId;
    }

    public void setBookingCancelId(int bookingCancelId) {
        this.bookingCancelId = bookingCancelId;
    }

    // Alias để khớp với BookingCancellationDAO nếu DAO gọi getCancellationId()
    public int getCancellationId() {
        return bookingCancelId;
    }

    // Alias để khớp với BookingCancellationDAO nếu DAO gọi setCancellationId()
    public void setCancellationId(int cancellationId) {
        this.bookingCancelId = cancellationId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getCancelBy() {
        return cancelBy;
    }

    public void setCancelBy(String cancelBy) {
        this.cancelBy = cancelBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public BigDecimal getRefundPercent() {
        return refundPercent;
    }

    public void setRefundPercent(BigDecimal refundPercent) {
        this.refundPercent = refundPercent;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BookingCancellation{" +
                "bookingCancelId=" + bookingCancelId +
                ", bookingId=" + bookingId +
                ", cancelBy='" + cancelBy + '\'' +
                ", cancelReason='" + cancelReason + '\'' +
                ", refundPercent=" + refundPercent +
                ", refundAmount=" + refundAmount +
                ", cancelledAt=" + cancelledAt +
                ", createdAt=" + createdAt +
                '}';
    }
}