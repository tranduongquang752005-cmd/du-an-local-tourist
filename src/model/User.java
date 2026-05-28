package model;

import java.time.LocalDateTime;

public class User {
    public enum Role {
        CUSTOMER,
        STAFF,
        MANAGER
    }

    private int userId;
    private String fullName;
    private String phone;
    private String loginName;
    private String passwordHash;
    private Role role;
    private boolean active;
    private int maxPendingBookings;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(int userId,
                String fullName,
                String phone,
                String loginName,
                String passwordHash,
                Role role,
                boolean active,
                int maxPendingBookings,
                LocalDateTime createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.loginName = loginName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.maxPendingBookings = maxPendingBookings;
        this.createdAt = createdAt;
    }

    /*
     * Constructor cũ để tránh code cũ đang new User(...) bị lỗi compile.
     * LoginName sẽ mặc định là null.
     */
    public User(int userId,
                String fullName,
                String phone,
                String passwordHash,
                Role role,
                boolean active,
                int maxPendingBookings,
                LocalDateTime createdAt) {
        this(
                userId,
                fullName,
                phone,
                null,
                passwordHash,
                role,
                active,
                maxPendingBookings,
                createdAt
        );
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = cleanString(fullName);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = cleanString(phone);
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        String cleaned = cleanString(loginName);
        this.loginName = cleaned == null ? null : cleaned.toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = cleanString(passwordHash);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setRole(String role) {
        String cleaned = cleanString(role);

        if (cleaned == null) {
            this.role = null;
            return;
        }

        try {
            this.role = Role.valueOf(cleaned.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.role = null;
        }
    }

    public String getRoleName() {
        return role == null ? null : role.name();
    }

    public boolean hasRole(Role expectedRole) {
        return role != null && role == expectedRole;
    }

    public boolean hasRole(String expectedRole) {
        String cleaned = cleanString(expectedRole);
        return role != null && cleaned != null && role.name().equalsIgnoreCase(cleaned);
    }

    public boolean isCustomer() {
        return hasRole(Role.CUSTOMER);
    }

    public boolean isStaff() {
        return hasRole(Role.STAFF);
    }

    public boolean isManager() {
        return hasRole(Role.MANAGER);
    }

    public boolean isInternalAccount() {
        return isStaff() || isManager();
    }

    public boolean isCustomerAccount() {
        return isCustomer();
    }

    public boolean hasLoginName() {
        return loginName != null && !loginName.trim().isEmpty();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getMaxPendingBookings() {
        return maxPendingBookings;
    }

    public void setMaxPendingBookings(int maxPendingBookings) {
        this.maxPendingBookings = maxPendingBookings;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", loginName='" + loginName + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", maxPendingBookings=" + maxPendingBookings +
                ", createdAt=" + createdAt +
                '}';
    }
}
