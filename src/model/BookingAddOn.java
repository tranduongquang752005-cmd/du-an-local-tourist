package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingAddOn {
    private int bookingAddOnId;
    private int bookingId;
    private int addOnId;
    private int quantity;
    private BigDecimal price;
    private LocalDateTime createdAt;

    // Field này không có trực tiếp trong bảng BOOKING_ADD_ONS,
    // nhưng lấy từ JOIN ADD_ONS để hiển thị tên dịch vụ.
    private String addOnName;

    public BookingAddOn() {
    }

    public BookingAddOn(int bookingAddOnId, int bookingId, int addOnId,
                        int quantity, BigDecimal price, LocalDateTime createdAt) {
        this.bookingAddOnId = bookingAddOnId;
        this.bookingId = bookingId;
        this.addOnId = addOnId;
        this.quantity = quantity;
        this.price = price;
        this.createdAt = createdAt;
    }

    public int getBookingAddOnId() {
        return bookingAddOnId;
    }

    public void setBookingAddOnId(int bookingAddOnId) {
        this.bookingAddOnId = bookingAddOnId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getAddOnId() {
        return addOnId;
    }

    public void setAddOnId(int addOnId) {
        this.addOnId = addOnId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAddOnName() {
        return addOnName;
    }

    public void setAddOnName(String addOnName) {
        this.addOnName = addOnName;
    }

    public BigDecimal getTotalPrice() {
        if (price == null) {
            return BigDecimal.ZERO;
        }

        return price.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return "BookingAddOn{" +
                "bookingAddOnId=" + bookingAddOnId +
                ", bookingId=" + bookingId +
                ", addOnId=" + addOnId +
                ", addOnName='" + addOnName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", totalPrice=" + getTotalPrice() +
                ", createdAt=" + createdAt +
                '}';
    }
}