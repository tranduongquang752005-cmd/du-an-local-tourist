package dao;

import config.DatabaseConnection;
import model.Coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO {

    private static final String TYPE_PERCENTAGE = "PERCENTAGE";
    private static final String TYPE_FIXED = "FIXED";

    private static final String BOOKING_STATUS_PENDING = "PENDING";

    private static final int MAX_COUPON_CODE_LENGTH = 50;
    private static final int MAX_DISCOUNT_TYPE_LENGTH = 20;

    public int createCoupon(String couponCode,
                            String discountType,
                            BigDecimal discountValue,
                            int maxUsagePerUser,
                            Integer maxTotalUsage,
                            BigDecimal maxDiscountAmount,
                            LocalDate expiryDate,
                            Integer createdBy) {
        CouponInput input = validateCouponInput(
                couponCode,
                discountType,
                discountValue,
                maxUsagePerUser,
                maxTotalUsage,
                maxDiscountAmount,
                expiryDate,
                createdBy
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (getCouponByCode(input.couponCode) != null) {
            System.out.println("CouponCode da ton tai.");
            return -1;
        }

        if (input.createdBy != null && !isUserExists(input.createdBy)) {
            System.out.println("CreatedBy khong ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO COUPONS
                (
                    CouponCode,
                    DiscountType,
                    DiscountValue,
                    MaxUsagePerUser,
                    MaxTotalUsage,
                    CurrentTotalUsage,
                    MaxDiscountAmount,
                    ExpiryDate,
                    IsActive,
                    CreatedBy
                )
                VALUES (?, ?, ?, ?, ?, 0, ?, ?, 1, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, input.couponCode);
            ps.setString(2, input.discountType);
            ps.setBigDecimal(3, input.discountValue);
            ps.setInt(4, input.maxUsagePerUser);
            setNullableInt(ps, 5, input.maxTotalUsage);
            setNullableBigDecimal(ps, 6, input.maxDiscountAmount);
            ps.setDate(7, Date.valueOf(input.expiryDate));
            setNullableInt(ps, 8, input.createdBy);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createCoupon");
        }

        return -1;
    }

    public int createCoupon(String couponCode,
                            String discountType,
                            BigDecimal discountValue,
                            int maxUsagePerUser,
                            Integer maxTotalUsage,
                            BigDecimal maxDiscountAmount,
                            LocalDate expiryDate) {
        return createCoupon(
                couponCode,
                discountType,
                discountValue,
                maxUsagePerUser,
                maxTotalUsage,
                maxDiscountAmount,
                expiryDate,
                null
        );
    }

    public boolean updateCoupon(int couponId,
                                String couponCode,
                                String discountType,
                                BigDecimal discountValue,
                                int maxUsagePerUser,
                                Integer maxTotalUsage,
                                BigDecimal maxDiscountAmount,
                                LocalDate expiryDate,
                                boolean active,
                                Integer createdBy) {
        if (couponId <= 0) {
            System.out.println("CouponID khong hop le.");
            return false;
        }

        Coupon current = getCouponById(couponId);

        if (current == null) {
            System.out.println("Khong tim thay coupon.");
            return false;
        }

        CouponInput input = validateCouponInput(
                couponCode,
                discountType,
                discountValue,
                maxUsagePerUser,
                maxTotalUsage,
                maxDiscountAmount,
                expiryDate,
                createdBy
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        Coupon duplicated = getCouponByCode(input.couponCode);

        if (duplicated != null && duplicated.getCouponId() != couponId) {
            System.out.println("CouponCode da ton tai o coupon khac.");
            return false;
        }

        if (input.maxTotalUsage != null && current.getCurrentTotalUsage() > input.maxTotalUsage) {
            System.out.println("MaxTotalUsage khong duoc nho hon CurrentTotalUsage hien tai.");
            return false;
        }

        if (input.createdBy != null && !isUserExists(input.createdBy)) {
            System.out.println("CreatedBy khong ton tai.");
            return false;
        }

        String sql = """
                UPDATE COUPONS
                SET CouponCode = ?,
                    DiscountType = ?,
                    DiscountValue = ?,
                    MaxUsagePerUser = ?,
                    MaxTotalUsage = ?,
                    MaxDiscountAmount = ?,
                    ExpiryDate = ?,
                    IsActive = ?,
                    CreatedBy = ?
                WHERE CouponID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, input.couponCode);
            ps.setString(2, input.discountType);
            ps.setBigDecimal(3, input.discountValue);
            ps.setInt(4, input.maxUsagePerUser);
            setNullableInt(ps, 5, input.maxTotalUsage);
            setNullableBigDecimal(ps, 6, input.maxDiscountAmount);
            ps.setDate(7, Date.valueOf(input.expiryDate));
            ps.setBoolean(8, active);
            setNullableInt(ps, 9, input.createdBy);
            ps.setInt(10, couponId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat coupon that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateCoupon");
        }

        return false;
    }

    public boolean updateCoupon(int couponId,
                                String discountType,
                                BigDecimal discountValue,
                                int maxUsagePerUser,
                                Integer maxTotalUsage,
                                BigDecimal maxDiscountAmount,
                                LocalDate expiryDate,
                                boolean active) {
        Coupon current = getCouponById(couponId);

        if (current == null) {
            System.out.println("Khong tim thay coupon.");
            return false;
        }

        return updateCoupon(
                couponId,
                current.getCouponCode(),
                discountType,
                discountValue,
                maxUsagePerUser,
                maxTotalUsage,
                maxDiscountAmount,
                expiryDate,
                active,
                current.getCreatedBy()
        );
    }

    public boolean activateCoupon(int couponId) {
        return updateActiveStatus(couponId, true);
    }

    public boolean deactivateCoupon(int couponId) {
        return updateActiveStatus(couponId, false);
    }

    public Coupon getCouponById(int couponId) {
        if (couponId <= 0) {
            System.out.println("CouponID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE CouponID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, couponId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCoupon(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getCouponById");
        }

        return null;
    }

    public Coupon getCouponByCode(String couponCode) {
        String cleanCode = normalizeCouponCode(couponCode);

        if (cleanCode == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE CouponCode = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCoupon(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getCouponByCode");
        }

        return null;
    }

    public List<Coupon> getAllCoupons() {
        String sql = buildSelectSql("""
                ORDER BY CreatedAt DESC, CouponID DESC
                """);

        return queryCouponList(sql, null, "getAllCoupons");
    }

    public List<Coupon> getActiveCoupons() {
        String sql = buildSelectSql("""
                WHERE IsActive = 1
                  AND ExpiryDate >= CAST(GETDATE() AS DATE)
                  AND (MaxTotalUsage IS NULL OR CurrentTotalUsage < MaxTotalUsage)
                ORDER BY ExpiryDate ASC, CouponID DESC
                """);

        return queryCouponList(sql, null, "getActiveCoupons");
    }

    public List<Coupon> getExpiredCoupons() {
        String sql = buildSelectSql("""
                WHERE ExpiryDate < CAST(GETDATE() AS DATE)
                ORDER BY ExpiryDate DESC, CouponID DESC
                """);

        return queryCouponList(sql, null, "getExpiredCoupons");
    }

    public CouponCheckResult checkCouponForUser(String couponCode, int userId, BigDecimal totalPrice) {
        String cleanCode = normalizeCouponCode(couponCode);

        if (cleanCode == null) {
            return CouponCheckResult.invalid("CouponCode khong hop le.");
        }

        if (userId <= 0) {
            return CouponCheckResult.invalid("UserID khong hop le.");
        }

        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return CouponCheckResult.invalid("TotalPrice phai lon hon 0.");
        }

        Coupon coupon = getCouponByCode(cleanCode);

        if (coupon == null) {
            return CouponCheckResult.invalid("Coupon khong ton tai.");
        }

        if (!coupon.isActive()) {
            return CouponCheckResult.invalid("Coupon dang bi tat.");
        }

        if (coupon.getExpiryDate() == null || coupon.getExpiryDate().isBefore(LocalDate.now())) {
            return CouponCheckResult.invalid("Coupon da het han.");
        }

        if (coupon.getMaxTotalUsage() != null
                && coupon.getCurrentTotalUsage() >= coupon.getMaxTotalUsage()) {
            return CouponCheckResult.invalid("Coupon da het tong luot su dung.");
        }

        int userUsageCount = getUserCouponUsageCount(userId, coupon.getCouponId());

        if (userUsageCount >= coupon.getMaxUsagePerUser()) {
            return CouponCheckResult.invalid("User da dung coupon qua so lan cho phep.");
        }

        BigDecimal discountAmount = calculateDiscountAmount(
                totalPrice,
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount()
        );

        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return CouponCheckResult.invalid("Coupon khong tao ra giam gia hop le.");
        }

        return CouponCheckResult.valid(coupon, discountAmount);
    }

    public boolean canUseCoupon(String couponCode, int userId, BigDecimal totalPrice) {
        return checkCouponForUser(couponCode, userId, totalPrice).isValid();
    }

    public BigDecimal calculateDiscountAmount(BigDecimal totalPrice,
                                              String discountType,
                                              BigDecimal discountValue,
                                              BigDecimal maxDiscountAmount) {
        if (totalPrice == null
                || totalPrice.compareTo(BigDecimal.ZERO) <= 0
                || discountType == null
                || discountValue == null
                || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount;

        if (TYPE_PERCENTAGE.equalsIgnoreCase(discountType)) {
            discountAmount = totalPrice
                    .multiply(discountValue)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            if (maxDiscountAmount != null && discountAmount.compareTo(maxDiscountAmount) > 0) {
                discountAmount = maxDiscountAmount;
            }
        } else if (TYPE_FIXED.equalsIgnoreCase(discountType)) {
            discountAmount = discountValue;
        } else {
            return BigDecimal.ZERO;
        }

        if (discountAmount.compareTo(totalPrice) > 0) {
            discountAmount = totalPrice;
        }

        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean applyCouponToPendingBooking(int bookingId, String couponCode) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return false;
        }

        String cleanCode = normalizeCouponCode(couponCode);

        if (cleanCode == null) {
            System.out.println("CouponCode khong hop le.");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Khong ket noi duoc database.");
                return false;
            }

            conn.setAutoCommit(false);

            try {
                BookingSnapshot booking = getBookingSnapshotForUpdate(conn, bookingId);

                if (booking == null) {
                    conn.rollback();
                    System.out.println("Booking khong ton tai.");
                    return false;
                }

                if (!BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
                    conn.rollback();
                    System.out.println("Chi ap dung coupon khi booking con PENDING.");
                    return false;
                }

                if (booking.couponId != null) {
                    conn.rollback();
                    System.out.println("Booking da co coupon. Hay remove truoc.");
                    return false;
                }

                CouponCheckResult checkResult = checkCouponForUser(
                        cleanCode,
                        booking.userId,
                        booking.totalPrice
                );

                if (!checkResult.isValid()) {
                    conn.rollback();
                    System.out.println(checkResult.getMessage());
                    return false;
                }

                Coupon coupon = checkResult.getCoupon();

                String updateBookingSql = """
                        UPDATE BOOKINGS
                        SET CouponID = ?,
                            DiscountAmount = ?
                        WHERE BookingID = ?
                          AND Status = 'PENDING'
                        """;

                try (PreparedStatement ps = conn.prepareStatement(updateBookingSql)) {
                    ps.setInt(1, coupon.getCouponId());
                    ps.setBigDecimal(2, checkResult.getDiscountAmount());
                    ps.setInt(3, bookingId);

                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        System.out.println("Cap nhat coupon vao booking that bai.");
                        return false;
                    }
                }

                increaseCouponUsage(conn, booking.userId, coupon.getCouponId());
                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                handleException(e, "applyCouponToPendingBooking");
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            handleException(e, "applyCouponToPendingBooking");
        }

        return false;
    }

    public boolean removeCouponFromPendingBooking(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Khong ket noi duoc database.");
                return false;
            }

            conn.setAutoCommit(false);

            try {
                BookingSnapshot booking = getBookingSnapshotForUpdate(conn, bookingId);

                if (booking == null) {
                    conn.rollback();
                    System.out.println("Booking khong ton tai.");
                    return false;
                }

                if (!BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
                    conn.rollback();
                    System.out.println("Chi go coupon khi booking con PENDING.");
                    return false;
                }

                if (booking.couponId == null) {
                    conn.rollback();
                    System.out.println("Booking chua co coupon.");
                    return false;
                }

                String updateBookingSql = """
                        UPDATE BOOKINGS
                        SET CouponID = NULL,
                            DiscountAmount = 0
                        WHERE BookingID = ?
                          AND Status = 'PENDING'
                        """;

                try (PreparedStatement ps = conn.prepareStatement(updateBookingSql)) {
                    ps.setInt(1, bookingId);

                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        System.out.println("Go coupon khoi booking that bai.");
                        return false;
                    }
                }

                decreaseCouponUsage(conn, booking.userId, booking.couponId);
                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                handleException(e, "removeCouponFromPendingBooking");
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            handleException(e, "removeCouponFromPendingBooking");
        }

        return false;
    }

    public int getUserCouponUsageCount(int userId, int couponId) {
        if (userId <= 0 || couponId <= 0) {
            return 0;
        }

        String sql = """
                SELECT ISNULL(UsageCount, 0) AS Total
                FROM USER_COUPON_USAGE
                WHERE UserID = ?
                  AND CouponID = ?
                """;

        return queryCount(
                sql,
                ps -> {
                    ps.setInt(1, userId);
                    ps.setInt(2, couponId);
                },
                "getUserCouponUsageCount"
        );
    }

    private void increaseCouponUsage(Connection conn, int userId, int couponId) throws SQLException {
        String updateCouponSql = """
                UPDATE COUPONS
                SET CurrentTotalUsage = CurrentTotalUsage + 1
                WHERE CouponID = ?
                  AND (MaxTotalUsage IS NULL OR CurrentTotalUsage < MaxTotalUsage)
                """;

        try (PreparedStatement ps = conn.prepareStatement(updateCouponSql)) {
            ps.setInt(1, couponId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Coupon da het luot su dung.");
            }
        }

        String mergeSql = """
                MERGE USER_COUPON_USAGE AS target
                USING (SELECT ? AS UserID, ? AS CouponID) AS source
                ON target.UserID = source.UserID
                   AND target.CouponID = source.CouponID
                WHEN MATCHED THEN
                    UPDATE SET UsageCount = UsageCount + 1,
                               LastUsedAt = GETDATE()
                WHEN NOT MATCHED THEN
                    INSERT (UserID, CouponID, UsageCount, LastUsedAt)
                    VALUES (source.UserID, source.CouponID, 1, GETDATE());
                """;

        try (PreparedStatement ps = conn.prepareStatement(mergeSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, couponId);
            ps.executeUpdate();
        }
    }

    private void decreaseCouponUsage(Connection conn, int userId, int couponId) throws SQLException {
        String updateCouponSql = """
                UPDATE COUPONS
                SET CurrentTotalUsage = CASE
                    WHEN CurrentTotalUsage > 0 THEN CurrentTotalUsage - 1
                    ELSE 0
                END
                WHERE CouponID = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(updateCouponSql)) {
            ps.setInt(1, couponId);
            ps.executeUpdate();
        }

        String updateUsageSql = """
                UPDATE USER_COUPON_USAGE
                SET UsageCount = CASE
                    WHEN UsageCount > 0 THEN UsageCount - 1
                    ELSE 0
                END
                WHERE UserID = ?
                  AND CouponID = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(updateUsageSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, couponId);
            ps.executeUpdate();
        }
    }

    private BookingSnapshot getBookingSnapshotForUpdate(Connection conn, int bookingId) throws SQLException {
        String sql = """
                SELECT
                    UserID,
                    CouponID,
                    TotalPrice,
                    Status
                FROM BOOKINGS WITH (UPDLOCK, ROWLOCK)
                WHERE BookingID = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BookingSnapshot snapshot = new BookingSnapshot();

                    snapshot.userId = rs.getInt("UserID");

                    int couponId = rs.getInt("CouponID");
                    snapshot.couponId = rs.wasNull() ? null : couponId;

                    snapshot.totalPrice = rs.getBigDecimal("TotalPrice");
                    snapshot.status = rs.getString("Status");

                    return snapshot;
                }
            }
        }

        return null;
    }

    private boolean updateActiveStatus(int couponId, boolean active) {
        if (couponId <= 0) {
            System.out.println("CouponID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE COUPONS
                SET IsActive = ?
                WHERE CouponID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, couponId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay coupon.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateActiveStatus");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    CouponID,
                    CouponCode,
                    DiscountType,
                    DiscountValue,
                    MaxUsagePerUser,
                    MaxTotalUsage,
                    CurrentTotalUsage,
                    MaxDiscountAmount,
                    ExpiryDate,
                    IsActive,
                    CreatedBy,
                    CreatedAt
                FROM COUPONS
                """ + condition;
    }

    private List<Coupon> queryCouponList(String sql,
                                         SqlSetter setter,
                                         String methodName) {
        List<Coupon> coupons = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    coupons.add(mapCoupon(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return coupons;
    }

    private int queryCount(String sql,
                           SqlSetter setter,
                           String methodName) {
        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total");
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return 0;
    }

    private Coupon mapCoupon(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();

        coupon.setCouponId(rs.getInt("CouponID"));
        coupon.setCouponCode(rs.getString("CouponCode"));
        coupon.setDiscountType(rs.getString("DiscountType"));
        coupon.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        coupon.setMaxUsagePerUser(rs.getInt("MaxUsagePerUser"));

        int maxTotalUsage = rs.getInt("MaxTotalUsage");
        coupon.setMaxTotalUsage(rs.wasNull() ? null : maxTotalUsage);

        coupon.setCurrentTotalUsage(rs.getInt("CurrentTotalUsage"));
        coupon.setMaxDiscountAmount(rs.getBigDecimal("MaxDiscountAmount"));

        Date expiryDate = rs.getDate("ExpiryDate");
        if (expiryDate != null) {
            coupon.setExpiryDate(expiryDate.toLocalDate());
        }

        coupon.setActive(rs.getBoolean("IsActive"));

        int createdBy = rs.getInt("CreatedBy");
        coupon.setCreatedBy(rs.wasNull() ? null : createdBy);

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            coupon.setCreatedAt(createdAt.toLocalDateTime());
        }

        return coupon;
    }

    private CouponInput validateCouponInput(String couponCode,
                                            String discountType,
                                            BigDecimal discountValue,
                                            int maxUsagePerUser,
                                            Integer maxTotalUsage,
                                            BigDecimal maxDiscountAmount,
                                            LocalDate expiryDate,
                                            Integer createdBy) {
        String cleanCode = normalizeCouponCode(couponCode);
        String cleanType = normalizeDiscountType(discountType);

        if (cleanCode == null) {
            return CouponInput.invalid("CouponCode khong hop le, toi da 50 ky tu.");
        }

        if (cleanType == null) {
            return CouponInput.invalid("DiscountType chi chap nhan PERCENTAGE hoac FIXED.");
        }

        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return CouponInput.invalid("DiscountValue phai lon hon 0.");
        }

        if (TYPE_PERCENTAGE.equals(cleanType) && discountValue.compareTo(new BigDecimal("100")) > 0) {
            return CouponInput.invalid("DiscountValue dang PERCENTAGE khong nen vuot qua 100.");
        }

        if (maxUsagePerUser <= 0) {
            return CouponInput.invalid("MaxUsagePerUser phai lon hon 0.");
        }

        if (maxTotalUsage != null && maxTotalUsage <= 0) {
            return CouponInput.invalid("MaxTotalUsage phai lon hon 0 neu co.");
        }

        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            return CouponInput.invalid("MaxDiscountAmount khong duoc am.");
        }

        if (expiryDate == null) {
            return CouponInput.invalid("ExpiryDate khong duoc null.");
        }

        if (createdBy != null && createdBy <= 0) {
            return CouponInput.invalid("CreatedBy khong hop le.");
        }

        return CouponInput.valid(
                cleanCode,
                cleanType,
                discountValue.setScale(2, RoundingMode.HALF_UP),
                maxUsagePerUser,
                maxTotalUsage,
                normalizeNullableMoney(maxDiscountAmount),
                expiryDate,
                createdBy
        );
    }

    private BigDecimal normalizeNullableMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCouponCode(String couponCode) {
        String value = cleanString(couponCode);

        if (value == null || value.length() > MAX_COUPON_CODE_LENGTH) {
            return null;
        }

        return value.toUpperCase();
    }

    private String normalizeDiscountType(String discountType) {
        String value = cleanString(discountType);

        if (value == null || value.length() > MAX_DISCOUNT_TYPE_LENGTH) {
            return null;
        }

        value = value.toUpperCase();

        if (TYPE_PERCENTAGE.equals(value) || TYPE_FIXED.equals(value)) {
            return value;
        }

        return null;
    }

    private boolean isUserExists(int userId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE UserID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, userId),
                "isUserExists"
        ) > 0;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void setNullableBigDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, value);
        }
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": CreatedBy/CouponID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": CouponCode bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang COUPONS/USER_COUPON_USAGE.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu coupon vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": CouponCode/DiscountType qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang COUPONS hoac USER_COUPON_USAGE.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class BookingSnapshot {
        private int userId;
        private Integer couponId;
        private BigDecimal totalPrice;
        private String status;
    }

    private static class CouponInput {
        private final boolean valid;
        private final String message;
        private final String couponCode;
        private final String discountType;
        private final BigDecimal discountValue;
        private final int maxUsagePerUser;
        private final Integer maxTotalUsage;
        private final BigDecimal maxDiscountAmount;
        private final LocalDate expiryDate;
        private final Integer createdBy;

        private CouponInput(boolean valid,
                            String message,
                            String couponCode,
                            String discountType,
                            BigDecimal discountValue,
                            int maxUsagePerUser,
                            Integer maxTotalUsage,
                            BigDecimal maxDiscountAmount,
                            LocalDate expiryDate,
                            Integer createdBy) {
            this.valid = valid;
            this.message = message;
            this.couponCode = couponCode;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.maxUsagePerUser = maxUsagePerUser;
            this.maxTotalUsage = maxTotalUsage;
            this.maxDiscountAmount = maxDiscountAmount;
            this.expiryDate = expiryDate;
            this.createdBy = createdBy;
        }

        private static CouponInput valid(String couponCode,
                                         String discountType,
                                         BigDecimal discountValue,
                                         int maxUsagePerUser,
                                         Integer maxTotalUsage,
                                         BigDecimal maxDiscountAmount,
                                         LocalDate expiryDate,
                                         Integer createdBy) {
            return new CouponInput(
                    true,
                    null,
                    couponCode,
                    discountType,
                    discountValue,
                    maxUsagePerUser,
                    maxTotalUsage,
                    maxDiscountAmount,
                    expiryDate,
                    createdBy
            );
        }

        private static CouponInput invalid(String message) {
            return new CouponInput(
                    false,
                    message,
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public static class CouponCheckResult {
        private final boolean valid;
        private final String message;
        private final Coupon coupon;
        private final BigDecimal discountAmount;

        private CouponCheckResult(boolean valid,
                                  String message,
                                  Coupon coupon,
                                  BigDecimal discountAmount) {
            this.valid = valid;
            this.message = message;
            this.coupon = coupon;
            this.discountAmount = discountAmount;
        }

        public static CouponCheckResult valid(Coupon coupon, BigDecimal discountAmount) {
            return new CouponCheckResult(true, "Coupon hop le.", coupon, discountAmount);
        }

        public static CouponCheckResult invalid(String message) {
            return new CouponCheckResult(false, message, null, BigDecimal.ZERO);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public Coupon getCoupon() {
            return coupon;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        @Override
        public String toString() {
            return "CouponCheckResult{" +
                    "valid=" + valid +
                    ", message='" + message + '\'' +
                    ", coupon=" + coupon +
                    ", discountAmount=" + discountAmount +
                    '}';
        }
    }
}
