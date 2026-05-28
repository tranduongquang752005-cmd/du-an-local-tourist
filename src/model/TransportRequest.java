package model;

import java.time.LocalDateTime;

public class TransportRequest {
    private int transportRequestId;
    private int scheduleId;
    private Integer bookingId;
    private String partnerName;
    private String contactPhone;
    private String pickupLocation;
    private String dropoffLocation;
    private int passengerCount;
    private String note;
    private String status;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TransportRequest() {
    }


    public TransportRequest(int transportRequestId, int scheduleId, Integer bookingId, String partnerName, String contactPhone, String pickupLocation, String dropoffLocation, int passengerCount, String note, String status, Integer createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.transportRequestId = transportRequestId;
        this.scheduleId = scheduleId;
        this.bookingId = bookingId;
        this.partnerName = partnerName;
        this.contactPhone = contactPhone;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.passengerCount = passengerCount;
        this.note = note;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public int getTransportRequestId() {
        return transportRequestId;
    }

    public void setTransportRequestId(int transportRequestId) {
        this.transportRequestId = transportRequestId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDropoffLocation() {
        return dropoffLocation;
    }

    public void setDropoffLocation(String dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TransportRequest{transportRequestId=" + transportRequestId +
                ", scheduleId=" + scheduleId +
                ", bookingId=" + bookingId +
                ", partnerName=" + partnerName +
                ", contactPhone=" + contactPhone +
                ", pickupLocation=" + pickupLocation +
                ", dropoffLocation=" + dropoffLocation +
                ", passengerCount=" + passengerCount +
                ", note=" + note +
                ", status=" + status +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
