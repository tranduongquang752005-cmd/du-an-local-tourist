package model;

import java.time.LocalDateTime;

public class OutboxEvent {
    private long outboxEventId;
    private String eventType;
    private String aggregateType;
    private int aggregateId;
    private String payload;
    private String eventKey;
    private Integer actorId;
    private String status;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public OutboxEvent() {
    }


    public OutboxEvent(long outboxEventId, String eventType, String aggregateType, int aggregateId, String payload, String eventKey, Integer actorId, String status, int retryCount, LocalDateTime nextRetryAt, String lastError, LocalDateTime createdAt, LocalDateTime publishedAt) {
        this.outboxEventId = outboxEventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.eventKey = eventKey;
        this.actorId = actorId;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }


    public long getOutboxEventId() {
        return outboxEventId;
    }

    public void setOutboxEventId(long outboxEventId) {
        this.outboxEventId = outboxEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public int getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(int aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public Integer getActorId() {
        return actorId;
    }

    public void setActorId(Integer actorId) {
        this.actorId = actorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    @Override
    public String toString() {
        return "OutboxEvent{outboxEventId=" + outboxEventId +
                ", eventType=" + eventType +
                ", aggregateType=" + aggregateType +
                ", aggregateId=" + aggregateId +
                ", payload=" + payload +
                ", eventKey=" + eventKey +
                ", actorId=" + actorId +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", nextRetryAt=" + nextRetryAt +
                ", lastError=" + lastError +
                ", createdAt=" + createdAt +
                ", publishedAt=" + publishedAt +
                '}';
    }
}
