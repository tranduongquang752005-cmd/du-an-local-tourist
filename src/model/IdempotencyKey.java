package model;

import java.time.LocalDateTime;

public class IdempotencyKey {
    private long idempotencyKeyId;
    private String idempotencyKey;
    private String operationType;
    private Integer userId;
    private String requestHash;
    private String status;
    private String responseCode;
    private String responseBody;
    private String resourceType;
    private Integer resourceId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IdempotencyKey() {
    }


    public IdempotencyKey(long idempotencyKeyId, String idempotencyKey, String operationType, Integer userId, String requestHash, String status, String responseCode, String responseBody, String resourceType, Integer resourceId, LocalDateTime expiresAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idempotencyKeyId = idempotencyKeyId;
        this.idempotencyKey = idempotencyKey;
        this.operationType = operationType;
        this.userId = userId;
        this.requestHash = requestHash;
        this.status = status;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public long getIdempotencyKeyId() {
        return idempotencyKeyId;
    }

    public void setIdempotencyKeyId(long idempotencyKeyId) {
        this.idempotencyKeyId = idempotencyKeyId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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
        return "IdempotencyKey{idempotencyKeyId=" + idempotencyKeyId +
                ", idempotencyKey=" + idempotencyKey +
                ", operationType=" + operationType +
                ", userId=" + userId +
                ", requestHash=" + requestHash +
                ", status=" + status +
                ", responseCode=" + responseCode +
                ", responseBody=" + responseBody +
                ", resourceType=" + resourceType +
                ", resourceId=" + resourceId +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
