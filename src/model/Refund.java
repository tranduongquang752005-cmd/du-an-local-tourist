package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Refund {
    private int refundId;
    private int bookingId;
    private BigDecimal refundAmount;
    private String refundMethod;
    private String refundStatus;
    private String transactionId;
    private LocalDateTime refundDate;
    private LocalDateTime createdAt;

    public Refund() {
    }


    public Refund(int refundId, int bookingId, BigDecimal refundAmount, String refundMethod, String refundStatus, String transactionId, LocalDateTime refundDate, LocalDateTime createdAt) {
        this.refundId = refundId;
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.refundMethod = refundMethod;
        this.refundStatus = refundStatus;
        this.transactionId = transactionId;
        this.refundDate = refundDate;
        this.createdAt = createdAt;
    }


    public int getRefundId() {
        return refundId;
    }

    public void setRefundId(int refundId) {
        this.refundId = refundId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getRefundDate() {
        return refundDate;
    }

    public void setRefundDate(LocalDateTime refundDate) {
        this.refundDate = refundDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Refund{refundId=" + refundId +
                ", bookingId=" + bookingId +
                ", refundAmount=" + refundAmount +
                ", refundMethod=" + refundMethod +
                ", refundStatus=" + refundStatus +
                ", transactionId=" + transactionId +
                ", refundDate=" + refundDate +
                ", createdAt=" + createdAt +
                '}';
    }
}
