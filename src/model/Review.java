package model;

import java.time.LocalDateTime;

public class Review {
    private int reviewId;
    private int userId;
    private int bookingId;
    private int rating;
    private String reviewContent;
    private LocalDateTime reviewDate;
    private LocalDateTime createdAt;
    private String userFullName;
    private String tourName;

    public Review() {
    }


    public Review(int reviewId, int userId, int bookingId, int rating, String reviewContent, LocalDateTime reviewDate, LocalDateTime createdAt, String userFullName, String tourName) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.bookingId = bookingId;
        this.rating = rating;
        this.reviewContent = reviewContent;
        this.reviewDate = reviewDate;
        this.createdAt = createdAt;
        this.userFullName = userFullName;
        this.tourName = tourName;
    }


    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReviewContent() {
        return reviewContent;
    }

    public void setReviewContent(String reviewContent) {
        this.reviewContent = reviewContent;
    }

    public LocalDateTime getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getTourName() {
        return tourName;
    }

    public void setTourName(String tourName) {
        this.tourName = tourName;
    }

    @Override
    public String toString() {
        return "Review{reviewId=" + reviewId +
                ", userId=" + userId +
                ", bookingId=" + bookingId +
                ", rating=" + rating +
                ", reviewContent=" + reviewContent +
                ", reviewDate=" + reviewDate +
                ", createdAt=" + createdAt +
                ", userFullName=" + userFullName +
                ", tourName=" + tourName +
                '}';
    }
}
