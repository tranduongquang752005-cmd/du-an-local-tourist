package model;

import java.time.LocalDateTime;

public class ETicket {
    private int ticketId;
    private int bookingId;
    private String ticketCode;
    private String qrCode;
    private String ticketStatus;
    private LocalDateTime issuedDate;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;

    public ETicket() {
    }


    public ETicket(int ticketId, int bookingId, String ticketCode, String qrCode, String ticketStatus, LocalDateTime issuedDate, LocalDateTime expiryDate, LocalDateTime createdAt) {
        this.ticketId = ticketId;
        this.bookingId = bookingId;
        this.ticketCode = ticketCode;
        this.qrCode = qrCode;
        this.ticketStatus = ticketStatus;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
    }


    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(String ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDateTime issuedDate) {
        this.issuedDate = issuedDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ETicket{ticketId=" + ticketId +
                ", bookingId=" + bookingId +
                ", ticketCode=" + ticketCode +
                ", qrCode=" + qrCode +
                ", ticketStatus=" + ticketStatus +
                ", issuedDate=" + issuedDate +
                ", expiryDate=" + expiryDate +
                ", createdAt=" + createdAt +
                '}';
    }
}
