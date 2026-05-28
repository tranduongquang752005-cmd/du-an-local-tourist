package service;

import model.User;

public class PermissionService {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_MANAGER = "MANAGER";

    public boolean isLoggedIn(User user) {
        return user != null;
    }

    public boolean isActive(User user) {
        return user != null && user.isActive();
    }

    public boolean isCustomer(User user) {
        return hasRole(user, ROLE_CUSTOMER);
    }

    public boolean isStaff(User user) {
        return hasRole(user, ROLE_STAFF);
    }

    public boolean isManager(User user) {
        return hasRole(user, ROLE_MANAGER);
    }

    public boolean isStaffOrManager(User user) {
        return isStaff(user) || isManager(user);
    }

    public boolean hasRole(User user, String expectedRole) {
        if (!isActive(user) || isBlank(expectedRole)) {
            return false;
        }

        String actualRole = getRoleName(user);
        return expectedRole.trim().equalsIgnoreCase(actualRole);
    }

    public String getRoleName(User user) {
        if (user == null || user.getRole() == null) {
            return "";
        }

        return String.valueOf(user.getRole()).trim().toUpperCase();
    }

    // =========================
    // CUSTOMER PERMISSIONS
    // =========================

    public boolean canViewPublicTours(User user) {
        return user == null || isActive(user);
    }

    public boolean canSearchTours(User user) {
        return user == null || isActive(user);
    }

    public boolean canViewTourBasicDetail(User user) {
        return user == null || isActive(user);
    }

    public boolean canViewTourImages(User user) {
        return user == null || isActive(user);
    }

    public boolean canViewTourReviews(User user) {
        return user == null || isActive(user);
    }

    public boolean canCreateBooking(User user) {
        return isCustomer(user);
    }

    public boolean canManageOwnPendingBooking(User user, int bookingUserId) {
        return isCustomer(user) && user.getUserId() == bookingUserId;
    }

    public boolean canApplyCouponToOwnBooking(User user, int bookingUserId) {
        return canManageOwnPendingBooking(user, bookingUserId);
    }

    public boolean canPayOwnBooking(User user, int bookingUserId) {
        return canManageOwnPendingBooking(user, bookingUserId);
    }

    public boolean canViewOwnTicket(User user, int ticketOwnerUserId) {
        return isCustomer(user) && user.getUserId() == ticketOwnerUserId;
    }

    public boolean canViewOwnBookings(User user) {
        return isCustomer(user);
    }

    public boolean canRequestOwnBookingCancellation(User user, int bookingUserId) {
        return isCustomer(user) && user.getUserId() == bookingUserId;
    }

    public boolean canCreateReview(User user, int bookingUserId) {
        return isCustomer(user) && user.getUserId() == bookingUserId;
    }

    /*
     * Tạm thời bỏ lịch trình/timeline chi tiết phía CUSTOMER theo yêu cầu hiện tại.
     * Nếu FE cần sau thì mở lại trong CustomerService.
     */
    public boolean canViewCustomerItineraryDetail(User user) {
        return false;
    }

    public boolean canViewCustomerTripTimelineDetail(User user) {
        return false;
    }

    // =========================
    // STAFF PERMISSIONS
    // =========================

    public boolean canViewOperationalBookings(User user) {
        return isStaffOrManager(user);
    }

    public boolean canViewBookingDetailForOperation(User user) {
        return isStaffOrManager(user);
    }

    public boolean canUpdateBookingOperationalStatus(User user) {
        return isStaffOrManager(user);
    }

    public boolean canViewBasicCustomerInfo(User user) {
        return isStaffOrManager(user);
    }

    public boolean canCreateTransportRequest(User user) {
        return isStaffOrManager(user);
    }

    public boolean canUpdateTransportRequest(User user) {
        return isStaffOrManager(user);
    }

    public boolean canSupportBookingCancellation(User user) {
        return isStaffOrManager(user);
    }

    public boolean canCreateRefundRequest(User user) {
        return isStaffOrManager(user);
    }

    public boolean canCompleteRefund(User user) {
        return isManager(user);
    }

    // =========================
    // MANAGER PERMISSIONS
    // Trưởng phòng là quyền cao nhất, dùng 100% chức năng quản trị.
    // =========================

    public boolean canManageTours(User user) {
        return isManager(user);
    }

    public boolean canManageTourCategories(User user) {
        return isManager(user);
    }

    public boolean canManageLocations(User user) {
        return isManager(user);
    }

    public boolean canManageTourLocations(User user) {
        return isManager(user);
    }

    public boolean canManageTourImages(User user) {
        return isManager(user);
    }

    public boolean canManageTourItinerary(User user) {
        return isManager(user);
    }

    public boolean canManageTourSchedules(User user) {
        return isManager(user);
    }

    public boolean canManageTourPrices(User user) {
        return isManager(user);
    }

    public boolean canManageDynamicPriceRules(User user) {
        return isManager(user);
    }

    public boolean canManageFuelPrices(User user) {
        return isManager(user);
    }

    public boolean canManageFeaturedTours(User user) {
        return isManager(user);
    }

    public boolean canManageAddOns(User user) {
        return isManager(user);
    }

    public boolean canManageCoupons(User user) {
        return isManager(user);
    }

    public boolean canManageUsers(User user) {
        return isManager(user);
    }

    public boolean canManageStaff(User user) {
        return isManager(user);
    }

    public boolean canViewRevenue(User user) {
        return isManager(user);
    }

    public boolean canViewDashboard(User user) {
        return isManager(user);
    }

    public boolean canViewAuditLog(User user) {
        return isManager(user);
    }

    public boolean canManageSystemConfig(User user) {
        return isManager(user);
    }

    public boolean canDeleteImportantData(User user) {
        return isManager(user);
    }

    // =========================
    // COMMON GUARDS
    // =========================

    public boolean requireCustomer(User user) {
        return requirePermission(canCreateBooking(user), "Chuc nang nay chi danh cho khach hang.");
    }

    public boolean requireStaffOrManager(User user) {
        return requirePermission(isStaffOrManager(user), "Chuc nang nay chi danh cho nhan vien hoac truong phong.");
    }

    public boolean requireManager(User user) {
        return requirePermission(isManager(user), "Chuc nang nay chi danh cho truong phong.");
    }

    public boolean requirePermission(boolean condition, String message) {
        if (!condition) {
            System.out.println(message);
            return false;
        }

        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
