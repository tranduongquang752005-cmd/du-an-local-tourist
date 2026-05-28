package model;

import java.time.LocalDateTime;

public class AuditLog {
    private int auditId;
    private String tableName;
    private int recordId;
    private String auditAction;
    private String oldStatus;
    private String newStatus;
    private Integer changedById;
    private String changedBy;
    private LocalDateTime changedAt;

    public AuditLog() {
    }


    public AuditLog(int auditId, String tableName, int recordId, String auditAction, String oldStatus, String newStatus, Integer changedById, String changedBy, LocalDateTime changedAt) {
        this.auditId = auditId;
        this.tableName = tableName;
        this.recordId = recordId;
        this.auditAction = auditAction;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedById = changedById;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }


    public int getAuditId() {
        return auditId;
    }

    public void setAuditId(int auditId) {
        this.auditId = auditId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public String getAuditAction() {
        return auditAction;
    }

    public void setAuditAction(String auditAction) {
        this.auditAction = auditAction;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public Integer getChangedById() {
        return changedById;
    }

    public void setChangedById(Integer changedById) {
        this.changedById = changedById;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    @Override
    public String toString() {
        return "AuditLog{auditId=" + auditId +
                ", tableName=" + tableName +
                ", recordId=" + recordId +
                ", auditAction=" + auditAction +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                ", changedById=" + changedById +
                ", changedBy=" + changedBy +
                ", changedAt=" + changedAt +
                '}';
    }
}
