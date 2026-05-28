package model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class TourItinerary {
    private int itineraryId;
    private int tourId;
    private int dayNumber;
    private LocalTime timeStart;
    private LocalTime timeEnd;
    private String activity;
    private String description;
    private LocalDateTime createdAt;

    public TourItinerary() {
    }


    public TourItinerary(int itineraryId, int tourId, int dayNumber, LocalTime timeStart, LocalTime timeEnd, String activity, String description, LocalDateTime createdAt) {
        this.itineraryId = itineraryId;
        this.tourId = tourId;
        this.dayNumber = dayNumber;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.activity = activity;
        this.description = description;
        this.createdAt = createdAt;
    }


    public int getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(int itineraryId) {
        this.itineraryId = itineraryId;
    }

    public int getTourId() {
        return tourId;
    }

    public void setTourId(int tourId) {
        this.tourId = tourId;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public LocalTime getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(LocalTime timeStart) {
        this.timeStart = timeStart;
    }

    public LocalTime getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(LocalTime timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TourItinerary{itineraryId=" + itineraryId +
                ", tourId=" + tourId +
                ", dayNumber=" + dayNumber +
                ", timeStart=" + timeStart +
                ", timeEnd=" + timeEnd +
                ", activity=" + activity +
                ", description=" + description +
                ", createdAt=" + createdAt +
                '}';
    }
}
