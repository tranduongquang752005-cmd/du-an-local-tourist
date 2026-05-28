package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingPassenger {
    private int passengerId;
    private int bookingId;
    private String passengerName;
    private String passengerType;
    private BigDecimal price;
    private int slotsOccupied;
    private LocalDateTime createdAt;

    public BookingPassenger() {
    }


    public BookingPassenger(int passengerId, int bookingId, String passengerName, String passengerType, BigDecimal price, int slotsOccupied, LocalDateTime createdAt) {
        this.passengerId = passengerId;
        this.bookingId = bookingId;
        this.passengerName = passengerName;
        this.passengerType = passengerType;
        this.price = price;
        this.slotsOccupied = slotsOccupied;
        this.createdAt = createdAt;
    }


    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerType() {
        return passengerType;
    }

    public void setPassengerType(String passengerType) {
        this.passengerType = passengerType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getSlotsOccupied() {
        return slotsOccupied;
    }

    public void setSlotsOccupied(int slotsOccupied) {
        this.slotsOccupied = slotsOccupied;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BookingPassenger{passengerId=" + passengerId +
                ", bookingId=" + bookingId +
                ", passengerName=" + passengerName +
                ", passengerType=" + passengerType +
                ", price=" + price +
                ", slotsOccupied=" + slotsOccupied +
                ", createdAt=" + createdAt +
                '}';
    }
}
