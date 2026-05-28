package service;

import dao.AuditLogDAO;
import dao.CouponDAO;
import dao.DynamicPriceRuleDAO;
import dao.FuelPriceDAO;
import dao.LocationDAO;
import dao.RefundDAO;
import dao.RevenueDAO;
import dao.SystemConfigDAO;
import dao.TourCategoryDAO;
import dao.TourDAO;
import dao.TourPriceDAO;

import model.AuditLog;
import model.Coupon;
import model.DynamicPriceRule;
import model.FuelPrice;
import model.Location;
import model.Refund;
import model.SystemConfig;
import model.Tour;
import model.TourCategory;
import model.TourPrice;
import model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ManagerService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private static final int MAX_REPORT_RANGE_DAYS = 3660;

    private static final int MAX_CODE_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_REASON_LENGTH = 255;
    private static final int MAX_CONFIG_KEY_LENGTH = 100;
    private static final int MAX_CONFIG_VALUE_LENGTH = 1000;
    private static final int MAX_TRANSACTION_ID_LENGTH = 100;
    private static final int MAX_TABLE_NAME_LENGTH = 50;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private static final Set<String> ALLOWED_DISCOUNT_TYPES = Set.of("PERCENTAGE", "FIXED");
    private static final Set<String> ALLOWED_DYNAMIC_CONDITION_TYPES = Set.of(
            "HOLIDAY",
            "WEEKEND",
            "FUEL_SURGE",
            "LOW_STOCK"
    );

    private final PermissionService permissionService;

    private final RevenueDAO revenueDAO;
    private final AuditLogDAO auditLogDAO;
    private final SystemConfigDAO systemConfigDAO;
    private final RefundDAO refundDAO;

    private final CouponDAO couponDAO;
    private final DynamicPriceRuleDAO dynamicPriceRuleDAO;
    private final FuelPriceDAO fuelPriceDAO;

    private final TourDAO tourDAO;
    private final TourPriceDAO tourPriceDAO;
    private final TourCategoryDAO tourCategoryDAO;
    private final LocationDAO locationDAO;

    public ManagerService() {
        this.permissionService = new PermissionService();

        this.revenueDAO = new RevenueDAO();
        this.auditLogDAO = new AuditLogDAO();
        this.systemConfigDAO = new SystemConfigDAO();
        this.refundDAO = new RefundDAO();

        this.couponDAO = new CouponDAO();
        this.dynamicPriceRuleDAO = new DynamicPriceRuleDAO();
        this.fuelPriceDAO = new FuelPriceDAO();

        this.tourDAO = new TourDAO();
        this.tourPriceDAO = new TourPriceDAO();
        this.tourCategoryDAO = new TourCategoryDAO();
        this.locationDAO = new LocationDAO();
    }

    // =========================
    // DASHBOARD / REVENUE
    // MANAGER only
    // =========================

    public ServiceResult<RevenueDAO.RevenueSummary> getRevenueSummary(User currentUser,
                                                                       LocalDate fromDate,
                                                                       LocalDate toDate) {
        ServiceResult<Boolean> permission = requireManagerRevenuePermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        DateRangeValidation dateRange = validateDateRange(fromDate, toDate);

        if (!dateRange.valid) {
            return ServiceResult.fail(dateRange.code, dateRange.message);
        }

        return safeExecute(
                () -> revenueDAO.getRevenueSummary(fromDate, toDate),
                "Lay tong quan doanh thu thanh cong.",
                "REVENUE_SUMMARY_FAILED",
                "Lay tong quan doanh thu that bai."
        );
    }

    public ServiceResult<List<RevenueDAO.TourRevenueRow>> getRevenueByTour(User currentUser,
                                                                           LocalDate fromDate,
                                                                           LocalDate toDate) {
        ServiceResult<Boolean> permission = requireManagerRevenuePermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        DateRangeValidation dateRange = validateDateRange(fromDate, toDate);

        if (!dateRange.valid) {
            return ServiceResult.fail(dateRange.code, dateRange.message);
        }

        return safeExecute(
                () -> revenueDAO.getRevenueByTour(fromDate, toDate),
                "Lay doanh thu theo tour thanh cong.",
                "REVENUE_BY_TOUR_FAILED",
                "Lay doanh thu theo tour that bai."
        );
    }

    public ServiceResult<List<RevenueDAO.MonthlyRevenueRow>> getMonthlyRevenue(User currentUser, int year) {
        ServiceResult<Boolean> permission = requireManagerRevenuePermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        if (year < MIN_YEAR || year > MAX_YEAR) {
            return ServiceResult.fail("YEAR_INVALID", "Nam thong ke khong hop le.");
        }

        return safeExecute(
                () -> revenueDAO.getMonthlyRevenue(year),
                "Lay doanh thu theo thang thanh cong.",
                "MONTHLY_REVENUE_FAILED",
                "Lay doanh thu theo thang that bai."
        );
    }

    public ServiceResult<List<RevenueDAO.PaymentRevenueRow>> getRecentSuccessfulPayments(User currentUser, int limit) {
        ServiceResult<Boolean> permission = requireManagerRevenuePermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        int safeLimit = normalizeLimit(limit);

        return safeExecute(
                () -> revenueDAO.getRecentSuccessfulPayments(safeLimit),
                "Lay payment thanh cong.",
                "RECENT_PAYMENT_FAILED",
                "Lay payment that bai."
        );
    }

    // =========================
    // AUDIT LOG
    // MANAGER only
    // =========================

    public ServiceResult<List<AuditLog>> getRecentAuditLogs(User currentUser, int limit) {
        ServiceResult<Boolean> permission = requireAuditPermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        int safeLimit = normalizeLimit(limit);

        return safeExecute(
                () -> auditLogDAO.getRecentAuditLogs(safeLimit),
                "Lay audit log gan day thanh cong.",
                "AUDIT_LOG_FAILED",
                "Lay audit log that bai."
        );
    }

    public ServiceResult<List<AuditLog>> getBookingAuditLogs(User currentUser, int bookingId) {
        ServiceResult<Boolean> permission = requireAuditPermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        return safeExecute(
                () -> auditLogDAO.getBookingAuditLogs(bookingId),
                "Lay audit log booking thanh cong.",
                "BOOKING_AUDIT_FAILED",
                "Lay audit log booking that bai."
        );
    }

    public ServiceResult<List<AuditLog>> getAuditLogsByTable(User currentUser, String tableName, int limit) {
        ServiceResult<Boolean> permission = requireAuditPermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        String cleanTableName = cleanString(tableName);

        if (!isValidSimpleName(cleanTableName, MAX_TABLE_NAME_LENGTH)) {
            return ServiceResult.fail("TABLE_INVALID", "TableName khong hop le.");
        }

        int safeLimit = normalizeLimit(limit);

        return safeExecute(
                () -> auditLogDAO.getAuditLogsByTable(cleanTableName, safeLimit),
                "Lay audit log theo bang thanh cong.",
                "AUDIT_BY_TABLE_FAILED",
                "Lay audit log theo bang that bai."
        );
    }

    public ServiceResult<Integer> countAuditLogsByTable(User currentUser, String tableName) {
        ServiceResult<Boolean> permission = requireAuditPermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        String cleanTableName = cleanString(tableName);

        if (!isValidSimpleName(cleanTableName, MAX_TABLE_NAME_LENGTH)) {
            return ServiceResult.fail("TABLE_INVALID", "TableName khong hop le.");
        }

        return safeExecute(
                () -> auditLogDAO.countAuditLogsByTable(cleanTableName),
                "Dem audit log thanh cong.",
                "AUDIT_COUNT_FAILED",
                "Dem audit log that bai."
        );
    }

    // =========================
    // SYSTEM CONFIG
    // MANAGER only
    // =========================

    public ServiceResult<SystemConfig> getConfigByKey(User currentUser, String configKey) {
        ServiceResult<Boolean> permission = requireSystemConfigPermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        String cleanKey = cleanString(configKey);

        if (!isValidConfigKey(cleanKey)) {
            return ServiceResult.fail("CONFIG_KEY_INVALID", "ConfigKey khong hop le.");
        }

        return safeExecuteWithNullCheck(
                () -> systemConfigDAO.getConfigByKey(cleanKey),
                "Lay cau hinh thanh cong.",
                "CONFIG_NOT_FOUND",
                "Khong tim thay cau hinh.",
                "CONFIG_GET_FAILED",
                "Lay cau hinh that bai."
        );
    }

    public ServiceResult<List<SystemConfig>> getActiveConfigs(User currentUser) {
        ServiceResult<Boolean> permission = requireSystemConfigPermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        return safeExecute(
                systemConfigDAO::getActiveConfigs,
                "Lay danh sach cau hinh active thanh cong.",
                "CONFIG_LIST_FAILED",
                "Lay danh sach cau hinh that bai."
        );
    }

    public ServiceResult<Boolean> updateConfigValue(User currentUser,
                                                    String configKey,
                                                    String configValue) {
        ServiceResult<Boolean> permission = requireSystemConfigPermission(currentUser);

        if (!permission.isSuccess()) {
            return permission;
        }

        String cleanKey = cleanString(configKey);
        String cleanValue = cleanString(configValue);

        if (!isValidConfigKey(cleanKey)) {
            return ServiceResult.fail("CONFIG_KEY_INVALID", "ConfigKey khong hop le.");
        }

        if (cleanValue == null || cleanValue.length() > MAX_CONFIG_VALUE_LENGTH) {
            return ServiceResult.fail("CONFIG_VALUE_INVALID", "ConfigValue khong hop le.");
        }

        SystemConfig currentConfig = systemConfigDAO.getConfigByKey(cleanKey);

        if (currentConfig == null) {
            return ServiceResult.fail("CONFIG_NOT_FOUND", "Khong tim thay cau hinh.");
        }

        boolean updated;

        try {
            updated = systemConfigDAO.updateConfigValue(
                    cleanKey,
                    cleanValue,
                    currentUser.getUserId()
            );
        } catch (Exception e) {
            return ServiceResult.fail("CONFIG_UPDATE_EXCEPTION", "Cap nhat cau hinh that bai: " + safeExceptionMessage(e));
        }

        if (!updated) {
            return ServiceResult.fail("CONFIG_UPDATE_FAILED", "Cap nhat cau hinh that bai.");
        }

        return ServiceResult.success(true, "Cap nhat cau hinh thanh cong.");
    }

    // =========================
    // REFUND
    // STAFF tạo request, MANAGER hoàn tất refund
    // =========================

    public ServiceResult<List<Refund>> getRecentRefunds(User currentUser, int limit) {
        ServiceResult<Boolean> permission = requireManagerRevenuePermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        int safeLimit = normalizeLimit(limit);

        return safeExecute(
                () -> refundDAO.getRecentRefunds(safeLimit),
                "Lay refund gan day thanh cong.",
                "RECENT_REFUND_FAILED",
                "Lay refund gan day that bai."
        );
    }

    public ServiceResult<List<Refund>> getRefundsByBooking(User currentUser, int bookingId) {
        ServiceResult<Boolean> permission = requireManagerRevenuePermission(currentUser);

        if (!permission.isSuccess()) {
            return ServiceResult.fail(permission.getCode(), permission.getMessage());
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        return safeExecute(
                () -> refundDAO.getRefundsByBookingId(bookingId),
                "Lay refund theo booking thanh cong.",
                "REFUND_BY_BOOKING_FAILED",
                "Lay refund theo booking that bai."
        );
    }

    public ServiceResult<Boolean> completeRefund(User currentUser, int refundId, String transactionId) {
        if (!permissionService.canCompleteRefund(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc hoan tat refund.");
        }

        if (refundId <= 0) {
            return ServiceResult.fail("REFUND_INVALID", "RefundID khong hop le.");
        }

        String cleanTransactionId = cleanString(transactionId);

        if (cleanTransactionId == null || cleanTransactionId.length() > MAX_TRANSACTION_ID_LENGTH) {
            return ServiceResult.fail("TRANSACTION_INVALID", "TransactionID refund khong hop le.");
        }

        boolean completed;

        try {
            completed = refundDAO.completeRefund(refundId, cleanTransactionId);
        } catch (Exception e) {
            return ServiceResult.fail("REFUND_COMPLETE_EXCEPTION", "Hoan tat refund that bai: " + safeExceptionMessage(e));
        }

        if (!completed) {
            return ServiceResult.fail("REFUND_COMPLETE_FAILED", "Hoan tat refund that bai.");
        }

        return ServiceResult.success(true, "Hoan tat refund thanh cong.");
    }

    // =========================
    // COUPON
    // MANAGER only
    // =========================

    public ServiceResult<List<Coupon>> getActiveCoupons(User currentUser) {
        if (!permissionService.canManageCoupons(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc quan ly coupon.");
        }

        return safeExecute(
                couponDAO::getActiveCoupons,
                "Lay coupon active thanh cong.",
                "COUPON_LIST_FAILED",
                "Lay coupon active that bai."
        );
    }

    public ServiceResult<Coupon> getCouponById(User currentUser, int couponId) {
        if (!permissionService.canManageCoupons(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc xem coupon.");
        }

        if (couponId <= 0) {
            return ServiceResult.fail("COUPON_INVALID", "CouponID khong hop le.");
        }

        return safeExecuteWithNullCheck(
                () -> couponDAO.getCouponById(couponId),
                "Lay coupon thanh cong.",
                "COUPON_NOT_FOUND",
                "Khong tim thay coupon.",
                "COUPON_GET_FAILED",
                "Lay coupon that bai."
        );
    }

    public ServiceResult<Integer> createCoupon(User currentUser,
                                               String couponCode,
                                               String discountType,
                                               BigDecimal discountValue,
                                               int maxUsagePerUser,
                                               Integer maxTotalUsage,
                                               BigDecimal maxDiscountAmount,
                                               LocalDate expiryDate) {
        if (!permissionService.canManageCoupons(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc tao coupon.");
        }

        CouponInputValidation validation = validateCouponInput(
                couponCode,
                discountType,
                discountValue,
                maxUsagePerUser,
                maxTotalUsage,
                maxDiscountAmount,
                expiryDate
        );

        if (!validation.valid) {
            return ServiceResult.fail(validation.code, validation.message);
        }

        int couponId;

        try {
            couponId = couponDAO.createCoupon(
                    validation.couponCode,
                    validation.discountType,
                    discountValue,
                    maxUsagePerUser,
                    maxTotalUsage,
                    maxDiscountAmount,
                    expiryDate,
                    currentUser.getUserId()
            );
        } catch (Exception e) {
            return ServiceResult.fail("COUPON_CREATE_EXCEPTION", "Tao coupon that bai: " + safeExceptionMessage(e));
        }

        if (couponId <= 0) {
            return ServiceResult.fail("COUPON_CREATE_FAILED", "Tao coupon that bai.");
        }

        return ServiceResult.success(couponId, "Tao coupon thanh cong.");
    }

    // =========================
    // DYNAMIC PRICE RULE
    // MANAGER only
    // =========================

    public ServiceResult<List<DynamicPriceRule>> getAllDynamicPriceRules(User currentUser) {
        if (!permissionService.canManageDynamicPriceRules(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc quan ly rule gia dong.");
        }

        return safeExecute(
                dynamicPriceRuleDAO::getAllRules,
                "Lay dynamic price rule thanh cong.",
                "RULE_LIST_FAILED",
                "Lay dynamic price rule that bai."
        );
    }

    public ServiceResult<Integer> createDynamicPriceRule(User currentUser,
                                                         String ruleName,
                                                         String conditionType,
                                                         BigDecimal modifierPercent,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         int priority) {
        if (!permissionService.canManageDynamicPriceRules(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc tao rule gia dong.");
        }

        RuleInputValidation validation = validateRuleInput(ruleName, conditionType, modifierPercent, startDate, endDate, priority);

        if (!validation.valid) {
            return ServiceResult.fail(validation.code, validation.message);
        }

        int ruleId;

        try {
            ruleId = dynamicPriceRuleDAO.createRule(
                    validation.ruleName,
                    validation.conditionType,
                    modifierPercent,
                    startDate,
                    endDate,
                    priority
            );
        } catch (Exception e) {
            return ServiceResult.fail("RULE_CREATE_EXCEPTION", "Tao rule gia dong that bai: " + safeExceptionMessage(e));
        }

        if (ruleId <= 0) {
            return ServiceResult.fail("RULE_CREATE_FAILED", "Tao rule gia dong that bai.");
        }

        return ServiceResult.success(ruleId, "Tao rule gia dong thanh cong.");
    }

    public ServiceResult<Boolean> updateDynamicPriceRule(User currentUser,
                                                         int ruleId,
                                                         String ruleName,
                                                         String conditionType,
                                                         BigDecimal modifierPercent,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         int priority) {
        if (!permissionService.canManageDynamicPriceRules(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc cap nhat rule gia dong.");
        }

        if (ruleId <= 0) {
            return ServiceResult.fail("RULE_INVALID", "RuleID khong hop le.");
        }

        RuleInputValidation validation = validateRuleInput(ruleName, conditionType, modifierPercent, startDate, endDate, priority);

        if (!validation.valid) {
            return ServiceResult.fail(validation.code, validation.message);
        }

        boolean updated;

        try {
            updated = dynamicPriceRuleDAO.updateRule(
                    ruleId,
                    validation.ruleName,
                    validation.conditionType,
                    modifierPercent,
                    startDate,
                    endDate,
                    priority
            );
        } catch (Exception e) {
            return ServiceResult.fail("RULE_UPDATE_EXCEPTION", "Cap nhat rule gia dong that bai: " + safeExceptionMessage(e));
        }

        if (!updated) {
            return ServiceResult.fail("RULE_UPDATE_FAILED", "Cap nhat rule gia dong that bai.");
        }

        return ServiceResult.success(true, "Cap nhat rule gia dong thanh cong.");
    }

    public ServiceResult<Boolean> deactivateDynamicPriceRule(User currentUser, int ruleId) {
        if (!permissionService.canManageDynamicPriceRules(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc tat rule gia dong.");
        }

        if (ruleId <= 0) {
            return ServiceResult.fail("RULE_INVALID", "RuleID khong hop le.");
        }

        boolean deactivated;

        try {
            deactivated = dynamicPriceRuleDAO.deactivateRule(ruleId);
        } catch (Exception e) {
            return ServiceResult.fail("RULE_DEACTIVATE_EXCEPTION", "Tat rule gia dong that bai: " + safeExceptionMessage(e));
        }

        if (!deactivated) {
            return ServiceResult.fail("RULE_DEACTIVATE_FAILED", "Tat rule gia dong that bai.");
        }

        return ServiceResult.success(true, "Tat rule gia dong thanh cong.");
    }

    // =========================
    // TOUR / PRICE / CATEGORY / LOCATION
    // MANAGER only for quản trị sâu
    // =========================

    public ServiceResult<List<Tour>> getAllActiveToursForManagement(User currentUser) {
        if (!permissionService.canManageTours(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc quan ly tour.");
        }

        return safeExecute(
                tourDAO::getAllActiveTours,
                "Lay tour active thanh cong.",
                "TOUR_LIST_FAILED",
                "Lay tour active that bai."
        );
    }

    public ServiceResult<List<TourPrice>> getTourPrices(User currentUser, int tourId) {
        if (!permissionService.canManageTourPrices(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc quan ly gia tour.");
        }

        if (tourId <= 0) {
            return ServiceResult.fail("TOUR_INVALID", "TourID khong hop le.");
        }

        return safeExecute(
                () -> tourPriceDAO.getPricesByTourId(tourId),
                "Lay gia tour thanh cong.",
                "TOUR_PRICE_LIST_FAILED",
                "Lay gia tour that bai."
        );
    }

    public ServiceResult<Integer> createTourPrice(User currentUser,
                                                  int tourId,
                                                  LocalDate effectiveDate,
                                                  BigDecimal price,
                                                  String reason) {
        if (!permissionService.canManageTourPrices(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc tao gia tour.");
        }

        String cleanReason = cleanString(reason);

        if (tourId <= 0) {
            return ServiceResult.fail("TOUR_INVALID", "TourID khong hop le.");
        }

        if (effectiveDate == null) {
            return ServiceResult.fail("EFFECTIVE_DATE_EMPTY", "EffectiveDate khong duoc null.");
        }

        if (price == null || price.compareTo(ZERO) < 0) {
            return ServiceResult.fail("PRICE_INVALID", "Gia tour khong hop le.");
        }

        if (cleanReason != null && cleanReason.length() > MAX_REASON_LENGTH) {
            return ServiceResult.fail("REASON_TOO_LONG", "Reason qua dai.");
        }

        int priceId;

        try {
            priceId = tourPriceDAO.createTourPrice(tourId, effectiveDate, price, cleanReason);
        } catch (Exception e) {
            return ServiceResult.fail("TOUR_PRICE_CREATE_EXCEPTION", "Tao gia tour that bai: " + safeExceptionMessage(e));
        }

        if (priceId <= 0) {
            return ServiceResult.fail("TOUR_PRICE_CREATE_FAILED", "Tao gia tour that bai.");
        }

        return ServiceResult.success(priceId, "Tao gia tour thanh cong.");
    }

    public ServiceResult<List<TourCategory>> getActiveCategories(User currentUser) {
        if (!permissionService.canManageTourCategories(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc quan ly danh muc tour.");
        }

        return safeExecute(
                tourCategoryDAO::getActiveCategories,
                "Lay danh muc tour thanh cong.",
                "CATEGORY_LIST_FAILED",
                "Lay danh muc tour that bai."
        );
    }

    public ServiceResult<Integer> createCategory(User currentUser, String categoryName, String description) {
        if (!permissionService.canManageTourCategories(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc tao danh muc.");
        }

        String cleanCategoryName = cleanString(categoryName);
        String cleanDescription = cleanString(description);

        if (cleanCategoryName == null || cleanCategoryName.length() > MAX_NAME_LENGTH) {
            return ServiceResult.fail("CATEGORY_NAME_INVALID", "Ten danh muc khong hop le.");
        }

        if (cleanDescription != null && cleanDescription.length() > 500) {
            return ServiceResult.fail("CATEGORY_DESCRIPTION_TOO_LONG", "Mo ta danh muc qua dai.");
        }

        int categoryId;

        try {
            categoryId = tourCategoryDAO.createCategory(cleanCategoryName, cleanDescription);
        } catch (Exception e) {
            return ServiceResult.fail("CATEGORY_CREATE_EXCEPTION", "Tao danh muc that bai: " + safeExceptionMessage(e));
        }

        if (categoryId <= 0) {
            return ServiceResult.fail("CATEGORY_CREATE_FAILED", "Tao danh muc that bai.");
        }

        return ServiceResult.success(categoryId, "Tao danh muc thanh cong.");
    }

    public ServiceResult<List<Location>> getAllLocations(User currentUser) {
        if (!permissionService.canManageLocations(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc quan ly dia diem.");
        }

        return safeExecute(
                locationDAO::getAllLocations,
                "Lay dia diem thanh cong.",
                "LOCATION_LIST_FAILED",
                "Lay dia diem that bai."
        );
    }

    public ServiceResult<Integer> createLocation(User currentUser, String locationName, String description) {
        if (!permissionService.canManageLocations(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc tao dia diem.");
        }

        String cleanLocationName = cleanString(locationName);
        String cleanDescription = cleanString(description);

        if (cleanLocationName == null || cleanLocationName.length() > 50) {
            return ServiceResult.fail("LOCATION_NAME_INVALID", "Ten dia diem khong hop le.");
        }

        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            return ServiceResult.fail("LOCATION_DESCRIPTION_TOO_LONG", "Mo ta dia diem qua dai.");
        }

        int locationId;

        try {
            locationId = locationDAO.createLocation(cleanLocationName, cleanDescription);
        } catch (Exception e) {
            return ServiceResult.fail("LOCATION_CREATE_EXCEPTION", "Tao dia diem that bai: " + safeExceptionMessage(e));
        }

        if (locationId <= 0) {
            return ServiceResult.fail("LOCATION_CREATE_FAILED", "Tao dia diem that bai.");
        }

        return ServiceResult.success(locationId, "Tao dia diem thanh cong.");
    }

    // =========================
    // FUEL PRICE READ
    // MANAGER can view full history
    // =========================

    public ServiceResult<List<FuelPrice>> getAllFuelPrices(User currentUser) {
        if (!permissionService.canManageFuelPrices(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc xem lich su gia xang.");
        }

        return safeExecute(
                fuelPriceDAO::getAllFuelPrices,
                "Lay danh sach gia xang thanh cong.",
                "FUEL_PRICE_LIST_FAILED",
                "Lay danh sach gia xang that bai."
        );
    }

    public ServiceResult<FuelPrice> getLatestFuelPrice(User currentUser) {
        if (!permissionService.canManageFuelPrices(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc xem gia xang.");
        }

        return safeExecuteWithNullCheck(
                fuelPriceDAO::getLatestFuelPrice,
                "Lay gia xang moi nhat thanh cong.",
                "FUEL_PRICE_NOT_FOUND",
                "Khong tim thay gia xang.",
                "FUEL_PRICE_GET_FAILED",
                "Lay gia xang that bai."
        );
    }

    private ServiceResult<Boolean> requireManagerRevenuePermission(User currentUser) {
        if (!permissionService.canViewRevenue(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc xem doanh thu.");
        }

        return ServiceResult.success(true, "OK");
    }

    private ServiceResult<Boolean> requireAuditPermission(User currentUser) {
        if (!permissionService.canViewAuditLog(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc xem audit log.");
        }

        return ServiceResult.success(true, "OK");
    }

    private ServiceResult<Boolean> requireSystemConfigPermission(User currentUser) {
        if (!permissionService.canManageSystemConfig(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi truong phong moi duoc quan ly cau hinh he thong.");
        }

        return ServiceResult.success(true, "OK");
    }

    private DateRangeValidation validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return DateRangeValidation.invalid("DATE_EMPTY", "FromDate/ToDate khong duoc null.");
        }

        if (fromDate.isAfter(toDate)) {
            return DateRangeValidation.invalid("DATE_RANGE_INVALID", "FromDate khong duoc lon hon ToDate.");
        }

        long days = Math.abs(toDate.toEpochDay() - fromDate.toEpochDay());

        if (days > MAX_REPORT_RANGE_DAYS) {
            return DateRangeValidation.invalid("DATE_RANGE_TOO_LARGE", "Khoang thoi gian thong ke qua lon.");
        }

        return DateRangeValidation.valid();
    }

    private CouponInputValidation validateCouponInput(String couponCode,
                                                      String discountType,
                                                      BigDecimal discountValue,
                                                      int maxUsagePerUser,
                                                      Integer maxTotalUsage,
                                                      BigDecimal maxDiscountAmount,
                                                      LocalDate expiryDate) {
        String cleanCouponCode = cleanString(couponCode);
        String cleanDiscountType = normalizeUpper(discountType);

        if (cleanCouponCode == null || cleanCouponCode.length() > MAX_CODE_LENGTH) {
            return CouponInputValidation.invalid("COUPON_CODE_INVALID", "CouponCode khong hop le.");
        }

        if (!cleanCouponCode.matches("^[A-Za-z0-9_\\-]+$")) {
            return CouponInputValidation.invalid("COUPON_CODE_FORMAT_INVALID", "CouponCode chi nen gom chu, so, gach ngang/gach duoi.");
        }

        if (!ALLOWED_DISCOUNT_TYPES.contains(cleanDiscountType)) {
            return CouponInputValidation.invalid("DISCOUNT_TYPE_INVALID", "DiscountType chi nhan PERCENTAGE hoac FIXED.");
        }

        if (discountValue == null || discountValue.compareTo(ZERO) <= 0) {
            return CouponInputValidation.invalid("DISCOUNT_VALUE_INVALID", "DiscountValue phai lon hon 0.");
        }

        if ("PERCENTAGE".equals(cleanDiscountType) && discountValue.compareTo(ONE_HUNDRED) > 0) {
            return CouponInputValidation.invalid("DISCOUNT_PERCENT_INVALID", "DiscountValue PERCENTAGE khong duoc lon hon 100.");
        }

        if (maxUsagePerUser <= 0) {
            return CouponInputValidation.invalid("MAX_USAGE_INVALID", "MaxUsagePerUser phai lon hon 0.");
        }

        if (maxTotalUsage != null && maxTotalUsage <= 0) {
            return CouponInputValidation.invalid("MAX_TOTAL_USAGE_INVALID", "MaxTotalUsage neu co phai lon hon 0.");
        }

        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(ZERO) < 0) {
            return CouponInputValidation.invalid("MAX_DISCOUNT_INVALID", "MaxDiscountAmount khong duoc am.");
        }

        if (expiryDate == null || expiryDate.isBefore(LocalDate.now())) {
            return CouponInputValidation.invalid("EXPIRY_INVALID", "ExpiryDate khong hop le.");
        }

        return CouponInputValidation.valid(cleanCouponCode, cleanDiscountType);
    }

    private RuleInputValidation validateRuleInput(String ruleName,
                                                  String conditionType,
                                                  BigDecimal modifierPercent,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  int priority) {
        String cleanRuleName = cleanString(ruleName);
        String cleanConditionType = normalizeUpper(conditionType);

        if (cleanRuleName == null || cleanRuleName.length() > MAX_NAME_LENGTH) {
            return RuleInputValidation.invalid("RULE_NAME_INVALID", "RuleName khong hop le.");
        }

        if (!ALLOWED_DYNAMIC_CONDITION_TYPES.contains(cleanConditionType)) {
            return RuleInputValidation.invalid("CONDITION_TYPE_INVALID", "ConditionType khong hop le.");
        }

        if (modifierPercent == null) {
            return RuleInputValidation.invalid("MODIFIER_EMPTY", "ModifierPercent khong duoc null.");
        }

        if (modifierPercent.compareTo(new BigDecimal("-100")) < 0 || modifierPercent.compareTo(new BigDecimal("999.99")) > 0) {
            return RuleInputValidation.invalid("MODIFIER_INVALID", "ModifierPercent khong hop le.");
        }

        if (priority < 1 || priority > 10) {
            return RuleInputValidation.invalid("PRIORITY_INVALID", "Priority phai tu 1 den 10.");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return RuleInputValidation.invalid("RULE_DATE_INVALID", "StartDate khong duoc lon hon EndDate.");
        }

        return RuleInputValidation.valid(cleanRuleName, cleanConditionType);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private boolean isValidSimpleName(String value, int maxLength) {
        return value != null
                && value.length() <= maxLength
                && value.matches("^[A-Za-z0-9_]+$");
    }

    private boolean isValidConfigKey(String value) {
        return value != null
                && value.length() <= MAX_CONFIG_KEY_LENGTH
                && value.matches("^[A-Za-z0-9_.\\-]+$");
    }

    private String normalizeUpper(String value) {
        String cleaned = cleanString(value);
        return cleaned == null ? null : cleaned.toUpperCase();
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeExceptionMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "Loi khong xac dinh.";
        }

        String message = e.getMessage().replaceAll("[\\r\\n\\t]+", " ").trim();

        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }

        return message;
    }

    private <T> ServiceResult<T> safeExecute(Supplier<T> supplier,
                                             String successMessage,
                                             String failCode,
                                             String failMessage) {
        try {
            T data = supplier.get();
            return ServiceResult.success(data, successMessage);
        } catch (Exception e) {
            return ServiceResult.fail(failCode, failMessage + ": " + safeExceptionMessage(e));
        }
    }

    private <T> ServiceResult<T> safeExecuteWithNullCheck(Supplier<T> supplier,
                                                          String successMessage,
                                                          String nullCode,
                                                          String nullMessage,
                                                          String failCode,
                                                          String failMessage) {
        try {
            T data = supplier.get();

            if (data == null) {
                return ServiceResult.fail(nullCode, nullMessage);
            }

            return ServiceResult.success(data, successMessage);
        } catch (Exception e) {
            return ServiceResult.fail(failCode, failMessage + ": " + safeExceptionMessage(e));
        }
    }

    private static class DateRangeValidation {
        private final boolean valid;
        private final String code;
        private final String message;

        private DateRangeValidation(boolean valid, String code, String message) {
            this.valid = valid;
            this.code = code;
            this.message = message;
        }

        private static DateRangeValidation valid() {
            return new DateRangeValidation(true, null, null);
        }

        private static DateRangeValidation invalid(String code, String message) {
            return new DateRangeValidation(false, code, message);
        }
    }

    private static class CouponInputValidation {
        private final boolean valid;
        private final String code;
        private final String message;
        private final String couponCode;
        private final String discountType;

        private CouponInputValidation(boolean valid, String code, String message, String couponCode, String discountType) {
            this.valid = valid;
            this.code = code;
            this.message = message;
            this.couponCode = couponCode;
            this.discountType = discountType;
        }

        private static CouponInputValidation valid(String couponCode, String discountType) {
            return new CouponInputValidation(true, null, null, couponCode, discountType);
        }

        private static CouponInputValidation invalid(String code, String message) {
            return new CouponInputValidation(false, code, message, null, null);
        }
    }

    private static class RuleInputValidation {
        private final boolean valid;
        private final String code;
        private final String message;
        private final String ruleName;
        private final String conditionType;

        private RuleInputValidation(boolean valid, String code, String message, String ruleName, String conditionType) {
            this.valid = valid;
            this.code = code;
            this.message = message;
            this.ruleName = ruleName;
            this.conditionType = conditionType;
        }

        private static RuleInputValidation valid(String ruleName, String conditionType) {
            return new RuleInputValidation(true, null, null, ruleName, conditionType);
        }

        private static RuleInputValidation invalid(String code, String message) {
            return new RuleInputValidation(false, code, message, null, null);
        }
    }

    public static class ServiceResult<T> {
        private final boolean success;
        private final String code;
        private final String message;
        private final T data;

        private ServiceResult(boolean success, String code, String message, T data) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public static <T> ServiceResult<T> success(T data, String message) {
            return new ServiceResult<>(true, "SUCCESS", message, data);
        }

        public static <T> ServiceResult<T> fail(String code, String message) {
            return new ServiceResult<>(false, code, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }

        @Override
        public String toString() {
            return "ServiceResult{" +
                    "success=" + success +
                    ", code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}
