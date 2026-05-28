package model;

import java.time.LocalDateTime;

public class BackgroundJob {
    private long jobId;
    private String jobType;
    private String payload;
    private int priority;
    private String status;
    private int attempts;
    private int maxAttempts;
    private LocalDateTime nextRunAt;
    private Integer triggeredById;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BackgroundJob() {
    }


    public BackgroundJob(long jobId, String jobType, String payload, int priority, String status, int attempts, int maxAttempts, LocalDateTime nextRunAt, Integer triggeredById, String lockedBy, LocalDateTime lockedAt, String lastError, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.payload = payload;
        this.priority = priority;
        this.status = status;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.nextRunAt = nextRunAt;
        this.triggeredById = triggeredById;
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public Integer getTriggeredById() {
        return triggeredById;
    }

    public void setTriggeredById(Integer triggeredById) {
        this.triggeredById = triggeredById;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "BackgroundJob{jobId=" + jobId +
                ", jobType=" + jobType +
                ", payload=" + payload +
                ", priority=" + priority +
                ", status=" + status +
                ", attempts=" + attempts +
                ", maxAttempts=" + maxAttempts +
                ", nextRunAt=" + nextRunAt +
                ", triggeredById=" + triggeredById +
                ", lockedBy=" + lockedBy +
                ", lockedAt=" + lockedAt +
                ", lastError=" + lastError +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
