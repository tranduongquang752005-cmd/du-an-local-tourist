package dao;

import config.DatabaseConnection;
import model.Booking;
import model.Booking.Status;
import util.AES256Util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.sql.CallableStatement;
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

public class BookingDAO {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String CANCEL_BY_CUSTOMER = "CUSTOMER";
    private static final String CANCEL_BY_COMPANY = "COMPANY";

    public int createBooking(int userId,
                             int tourId,
                             int scheduleId,
                             int adultCount,
                             int childCount,
                             int babyCount,
                             String couponCode) {
        BookingInput input = validateBookingInput(
                userId,
                tourId,
                scheduleId,
                adultCount,
                childCount,
                babyCount
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        String checkScheduleSql = """
                SELECT
                    ts.ScheduleID,
                    ts.TourID,
                    ts.ScheduleDate,
                    ts.AvailableSlots,
                    ts.BookedSlots,
                    ts.PriceMultiplier,
                    ts.Surcharge,
                    ISNULL(tp.Price, t.BasePrice) AS BasePrice
                FROM TOUR_SCHEDULES ts WITH (UPDLOCK, ROWLOCK)
                JOIN TOURS t ON t.TourID = ts.TourID
                OUTER APPLY (
                    SELECT TOP 1 Price
                    FROM TOUR_PRICES
                    WHERE TourID = ts.TourID
                      AND EffectiveDate <= ts.ScheduleDate
                    ORDER BY EffectiveDate DESC, PriceID DESC
                ) tp
                WHERE ts.ScheduleID = ?
                  AND ts.TourID = ?
                  AND t.IsActive = 1
                  AND ts.ScheduleDate >= CAST(GETDATE() AS DATE)
                """;

        String insertBookingSql = """
                INSERT INTO BOOKINGS
                (
                    UserID,
                    TourID,
                    ScheduleID,
                    TotalPrice,
                    CouponID,
                    DiscountAmount,
                    SurchargeAmount,
                    Status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """;

        String insertPassengerSql = """
                INSERT INTO BOOKING_PASSENGERS
                (
                    BookingID,
                    PassengerName,
                    PassengerType,
                    Price,
                    SlotsOccupied
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Khong ket noi duoc database.");
                return -1;
            }

            conn.setAutoCommit(false);

            try {
                if (!isActiveUser(conn, input.userId)) {
                    conn.rollback();
                    System.out.println("UserID khong ton tai hoac da bi vo hieu hoa.");
                    return -1;
                }

                if (countPendingBookingsByUser(conn, input.userId) >= getMaxPendingBookings(conn, input.userId)) {
                    conn.rollback();
                    System.out.println("User da dat toi gioi han booking PENDING.");
                    return -1;
                }

                ScheduleSnapshot schedule = getScheduleSnapshot(
                        conn,
                        checkScheduleSql,
                        input.scheduleId,
                        input.tourId
                );

                if (schedule == null) {
                    conn.rollback();
                    System.out.println("Lich khoi hanh khong hop le, da qua ngay hoac tour da ngung.");
                    return -1;
                }

                int neededSlots = input.adultCount + input.childCount;
                int remainingSlots = schedule.availableSlots - schedule.bookedSlots;

                if (remainingSlots < neededSlots) {
                    conn.rollback();
                    System.out.println("Khong du ghe trong. Con lai: " + remainingSlots);
                    return -2;
                }

                PriceSnapshot price = calculatePassengerPrices(schedule.basePrice, schedule.priceMultiplier);

                BigDecimal totalPrice = price.adultPrice.multiply(BigDecimal.valueOf(input.adultCount))
                        .add(price.childPrice.multiply(BigDecimal.valueOf(input.childCount)))
                        .add(price.babyPrice.multiply(BigDecimal.valueOf(input.babyCount)))
                        .setScale(2, RoundingMode.HALF_UP);

                CouponInfo couponInfo = getValidCouponInfo(
                        conn,
                        input.userId,
                        couponCode,
                        totalPrice
                );

                Integer couponId = couponInfo == null ? null : couponInfo.couponId;
                BigDecimal discountAmount = couponInfo == null
                        ? BigDecimal.ZERO
                        : couponInfo.discountAmount;

                int bookingId;

                try (
                        PreparedStatement ps = conn.prepareStatement(
                                insertBookingSql,
                                Statement.RETURN_GENERATED_KEYS
                        )
                ) {
                    ps.setInt(1, input.userId);
                    ps.setInt(2, input.tourId);
                    ps.setInt(3, input.scheduleId);
                    ps.setBigDecimal(4, totalPrice);
                    setNullableInt(ps, 5, couponId);
                    ps.setBigDecimal(6, discountAmount);
                    ps.setBigDecimal(7, schedule.surcharge);

                    int affectedRows = ps.executeUpdate();

                    if (affectedRows == 0) {
                        conn.rollback();
                        System.out.println("Tao booking that bai.");
                        return -1;
                    }

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            conn.rollback();
                            System.out.println("Khong lay duoc BookingID.");
                            return -1;
                        }

                        bookingId = rs.getInt(1);
                    }
                }

                insertPassengers(
                        conn,
                        insertPassengerSql,
                        bookingId,
                        input.adultCount,
                        input.childCount,
                        input.babyCount,
                        price.adultPrice,
                        price.childPrice,
                        price.babyPrice
                );

                if (couponInfo != null) {
                    updateCouponUsage(conn, input.userId, couponInfo.couponId);
                }

                conn.commit();
                return bookingId;

            } catch (SQLException e) {
                conn.rollback();

                if (e.getErrorCode() == 50001) {
                    System.out.println("Het ghe trong do co nguoi dat cung luc. Vui long thu lai.");
                    return -2;
                }

                handleException(e, "createBooking");
                return -1;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            handleException(e, "createBooking");
        }

        return -1;
    }

    /*
     * Giữ hàm này để code cũ/Main cũ không bị lỗi.
     * Luồng mới nên ưu tiên BookingCancellationDAO.cancelBooking(...)
     * vì DAO đó đầy đủ hơn cho nghiệp vụ refund/cancel.
     */
    public boolean cancelBooking(int bookingId,
                                 String cancelBy,
                                 String cancelReason,
                                 BigDecimal refundPercent) {
        String normalizedCancelBy = normalizeCancelBy(cancelBy);
        String cleanReason = cleanString(cancelReason);
        BigDecimal validRefundPercent = normalizeRefundPercent(refundPercent);

        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return false;
        }

        if (normalizedCancelBy == null) {
            System.out.println("CancelBy chi chap nhan CUSTOMER hoac COMPANY.");
            return false;
        }

        if (cleanReason == null || cleanReason.length() > 1000) {
            System.out.println("CancelReason khong hop le, toi da 1000 ky tu.");
            return false;
        }

        if (validRefundPercent == null) {
            System.out.println("RefundPercent phai tu 0 den 100.");
            return false;
        }

        String sql = """
                INSERT INTO BOOKING_CANCELLATIONS
                (
                    BookingID,
                    CancelBy,
                    CancelReason,
                    RefundPercent,
                    RefundAmount
                )
                SELECT
                    b.BookingID,
                    ?,
                    ?,
                    ?,
                    b.FinalPrice * ? / 100.0
                FROM BOOKINGS b
                WHERE b.BookingID = ?
                  AND b.Status IN ('PENDING', 'PAID')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM BOOKING_CANCELLATIONS bc
                      WHERE bc.BookingID = b.BookingID
                  )
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, normalizedCancelBy);
            ps.setString(2, cleanReason);
            ps.setBigDecimal(3, validRefundPercent);
            ps.setBigDecimal(4, validRefundPercent);
            ps.setInt(5, bookingId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Booking khong ton tai, da huy roi hoac khong o trang thai co the huy.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "cancelBooking");
        }

        return false;
    }

    public Booking getBookingById(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return null;
        }

        String sql = buildSelectBookingSql("""
                WHERE b.BookingID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBooking(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getBookingById");
        }

        return null;
    }

    public List<Booking> getBookingsByUserId(int userId) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectBookingSql("""
                WHERE b.UserID = ?
                ORDER BY b.CreatedAt DESC, b.BookingID DESC
                """);

        return queryBookings(
                sql,
                ps -> ps.setInt(1, userId),
                "getBookingsByUserId"
        );
    }

    public List<Booking> getCurrentBookingsByUserId(int userId) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectBookingSql("""
                WHERE b.UserID = ?
                  AND b.Status IN ('PENDING', 'PAID')
                ORDER BY b.CreatedAt DESC, b.BookingID DESC
                """);

        return queryBookings(
                sql,
                ps -> ps.setInt(1, userId),
                "getCurrentBookingsByUserId"
        );
    }

    public List<Booking> getBookingHistoryByUserId(int userId) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectBookingSql("""
                WHERE b.UserID = ?
                  AND b.Status IN ('COMPLETED', 'CANCELLED')
                ORDER BY b.CreatedAt DESC, b.BookingID DESC
                """);

        return queryBookings(
                sql,
                ps -> ps.setInt(1, userId),
                "getBookingHistoryByUserId"
        );
    }

    public List<Booking> getAllBookingsForStaff(int page, int pageSize) {
        int validPage = Math.max(page, 1);
        int validPageSize = normalizePageSize(pageSize);
        int offset = (validPage - 1) * validPageSize;

        String sql = buildSelectBookingSql("""
                ORDER BY b.CreatedAt DESC, b.BookingID DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """);

        return queryBookings(
                sql,
                ps -> {
                    ps.setInt(1, offset);
                    ps.setInt(2, validPageSize);
                },
                "getAllBookingsForStaff"
        );
    }

    public int countAllBookings() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                """;

        return queryInt(sql, null, "countAllBookings");
    }

    public int countBookingsByStatus(String status) {
        String normalizedStatus = normalizeBookingStatus(status);

        if (normalizedStatus == null) {
            System.out.println("Status booking khong hop le.");
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                WHERE Status = ?
                """;

        return queryInt(
                sql,
                ps -> ps.setString(1, normalizedStatus),
                "countBookingsByStatus"
        );
    }

    public List<Booking> getBookingsByStatus(String status, int page, int pageSize) {
        String normalizedStatus = normalizeBookingStatus(status);

        if (normalizedStatus == null) {
            System.out.println("Status booking khong hop le.");
            return new ArrayList<>();
        }

        int validPage = Math.max(page, 1);
        int validPageSize = normalizePageSize(pageSize);
        int offset = (validPage - 1) * validPageSize;

        String sql = buildSelectBookingSql("""
                WHERE b.Status = ?
                ORDER BY b.CreatedAt DESC, b.BookingID DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """);

        return queryBookings(
                sql,
                ps -> {
                    ps.setString(1, normalizedStatus);
                    ps.setInt(2, offset);
                    ps.setInt(3, validPageSize);
                },
                "getBookingsByStatus"
        );
    }

    public BigDecimal calculateTourPriceByProcedure(int tourId,
                                                    LocalDate scheduleDate,
                                                    int adultCount,
                                                    int childCount,
                                                    int babyCount) {
        if (tourId <= 0 || scheduleDate == null) {
            System.out.println("TourID hoac ScheduleDate khong hop le.");
            return BigDecimal.ZERO;
        }

        if (adultCount < 0 || childCount < 0 || babyCount < 0
                || adultCount + childCount + babyCount <= 0) {
            System.out.println("So luong hanh khach khong hop le.");
            return BigDecimal.ZERO;
        }

        String sql = "{CALL sp_GetTourPrice(?, ?, ?, ?, ?)}";

        try (
                Connection conn = DatabaseConnection.getConnection();
                CallableStatement cs = conn.prepareCall(sql)
        ) {
            cs.setInt(1, tourId);
            cs.setDate(2, Date.valueOf(scheduleDate));
            cs.setInt(3, adultCount);
            cs.setInt(4, childCount);
            cs.setInt(5, babyCount);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    BigDecimal finalPrice = rs.getBigDecimal("FinalPrice");
                    return finalPrice == null
                            ? BigDecimal.ZERO
                            : finalPrice.setScale(2, RoundingMode.HALF_UP);
                }
            }

        } catch (SQLException e) {
            handleException(e, "calculateTourPriceByProcedure");
        }

        return BigDecimal.ZERO;
    }

    private ScheduleSnapshot getScheduleSnapshot(Connection conn,
                                                 String sql,
                                                 int scheduleId,
                                                 int tourId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            ps.setInt(2, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ScheduleSnapshot snapshot = new ScheduleSnapshot();

                    snapshot.availableSlots = rs.getInt("AvailableSlots");
                    snapshot.bookedSlots = rs.getInt("BookedSlots");
                    snapshot.basePrice = rs.getBigDecimal("BasePrice");
                    snapshot.priceMultiplier = rs.getBigDecimal("PriceMultiplier");
                    snapshot.surcharge = rs.getBigDecimal("Surcharge");

                    if (snapshot.surcharge == null) {
                        snapshot.surcharge = BigDecimal.ZERO;
                    }

                    return snapshot;
                }
            }
        }

        return null;
    }

    private PriceSnapshot calculatePassengerPrices(BigDecimal basePrice, BigDecimal priceMultiplier) {
        BigDecimal adultPrice = basePrice
                .multiply(priceMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal childPrice = basePrice
                .multiply(new BigDecimal("0.50"))
                .multiply(priceMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal babyPrice = basePrice
                .multiply(new BigDecimal("0.25"))
                .multiply(priceMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        return new PriceSnapshot(adultPrice, childPrice, babyPrice);
    }

    private void insertPassengers(Connection conn,
                                  String insertPassengerSql,
                                  int bookingId,
                                  int adultCount,
                                  int childCount,
                                  int babyCount,
                                  BigDecimal adultPrice,
                                  BigDecimal childPrice,
                                  BigDecimal babyPrice) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(insertPassengerSql)) {
            for (int i = 1; i <= adultCount; i++) {
                insertPassenger(ps, bookingId, "Nguoi lon " + i, "ADULT", adultPrice, 1);
            }

            for (int i = 1; i <= childCount; i++) {
                insertPassenger(ps, bookingId, "Tre em " + i, "CHILD", childPrice, 1);
            }

            for (int i = 1; i <= babyCount; i++) {
                insertPassenger(ps, bookingId, "Em be " + i, "BABY", babyPrice, 0);
            }
        }
    }

    private void insertPassenger(PreparedStatement ps,
                                 int bookingId,
                                 String passengerName,
                                 String passengerType,
                                 BigDecimal price,
                                 int slotsOccupied) throws SQLException {
        ps.setInt(1, bookingId);
        ps.setString(2, passengerName);
        ps.setString(3, passengerType);
        ps.setBigDecimal(4, price);
        ps.setInt(5, slotsOccupied);
        ps.executeUpdate();
    }

    private CouponInfo getValidCouponInfo(Connection conn,
                                          int userId,
                                          String couponCode,
                                          BigDecimal totalPrice) throws SQLException {
        String cleanCouponCode = cleanString(couponCode);

        if (cleanCouponCode == null) {
            return null;
        }

        cleanCouponCode = cleanCouponCode.toUpperCase();

        String sql = """
                SELECT
                    c.CouponID,
                    c.DiscountType,
                    c.DiscountValue,
                    c.MaxUsagePerUser,
                    c.MaxTotalUsage,
                    c.CurrentTotalUsage,
                    c.MaxDiscountAmount,
                    c.ExpiryDate,
                    ISNULL(ucu.UsageCount, 0) AS UserUsageCount
                FROM COUPONS c WITH (UPDLOCK, ROWLOCK)
                LEFT JOIN USER_COUPON_USAGE ucu
                    ON ucu.CouponID = c.CouponID
                   AND ucu.UserID = ?
                WHERE c.CouponCode = ?
                  AND c.IsActive = 1
                  AND c.ExpiryDate >= CAST(GETDATE() AS DATE)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, cleanCouponCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Coupon khong hop le hoac da het han.");
                    return null;
                }

                Integer maxTotalUsage = getNullableInt(rs, "MaxTotalUsage");
                int currentTotalUsage = rs.getInt("CurrentTotalUsage");
                int maxUsagePerUser = rs.getInt("MaxUsagePerUser");
                int userUsageCount = rs.getInt("UserUsageCount");

                if (maxTotalUsage != null && currentTotalUsage >= maxTotalUsage) {
                    System.out.println("Coupon da het tong luot su dung.");
                    return null;
                }

                if (userUsageCount >= maxUsagePerUser) {
                    System.out.println("User da dung coupon qua so lan cho phep.");
                    return null;
                }

                BigDecimal discountAmount = calculateDiscountAmount(
                        totalPrice,
                        rs.getString("DiscountType"),
                        rs.getBigDecimal("DiscountValue"),
                        rs.getBigDecimal("MaxDiscountAmount")
                );

                if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Coupon khong tao ra giam gia hop le.");
                    return null;
                }

                return new CouponInfo(
                        rs.getInt("CouponID"),
                        discountAmount
                );
            }
        }
    }

    private BigDecimal calculateDiscountAmount(BigDecimal totalPrice,
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

        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            discountAmount = totalPrice
                    .multiply(discountValue)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            if (maxDiscountAmount != null
                    && discountAmount.compareTo(maxDiscountAmount) > 0) {
                discountAmount = maxDiscountAmount;
            }
        } else if ("FIXED".equalsIgnoreCase(discountType)) {
            discountAmount = discountValue;
        } else {
            return BigDecimal.ZERO;
        }

        if (discountAmount.compareTo(totalPrice) > 0) {
            discountAmount = totalPrice;
        }

        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private void updateCouponUsage(Connection conn, int userId, int couponId) throws SQLException {
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

        String mergeUsageSql = """
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

        try (PreparedStatement ps = conn.prepareStatement(mergeUsageSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, couponId);
            ps.executeUpdate();
        }
    }

    private boolean isActiveUser(Connection conn, int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE UserID = ?
                  AND IsActive = 1
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("Total") > 0;
            }
        }
    }

    private int countPendingBookingsByUser(Connection conn, int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                WHERE UserID = ?
                  AND Status = 'PENDING'
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("Total") : 0;
            }
        }
    }

    private int getMaxPendingBookings(Connection conn, int userId) throws SQLException {
        String sql = """
                SELECT ISNULL(MaxPendingBookings, 3) AS MaxPendingBookings
                FROM USERS
                WHERE UserID = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("MaxPendingBookings") : 3;
            }
        }
    }

    private String buildSelectBookingSql(String condition) {
        return """
                SELECT
                    b.BookingID,
                    b.UserID,
                    b.TourID,
                    b.ScheduleID,
                    b.BookingDate,
                    b.TotalPrice,
                    b.CouponID,
                    b.DiscountAmount,
                    b.SurchargeAmount,
                    b.FinalPrice,
                    b.Status,
                    b.CreatedAt,
                    t.TourName,
                    u.FullName AS UserFullName
                FROM BOOKINGS b
                JOIN TOURS t ON t.TourID = b.TourID
                JOIN USERS u ON u.UserID = b.UserID
                """ + condition;
    }

    private List<Booking> queryBookings(String sql,
                                        SqlSetter setter,
                                        String methodName) {
        List<Booking> bookings = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return bookings;
    }

    private int queryInt(String sql,
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

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking booking = new Booking();

        booking.setBookingId(rs.getInt("BookingID"));
        booking.setUserId(rs.getInt("UserID"));
        booking.setTourId(rs.getInt("TourID"));
        booking.setScheduleId(rs.getInt("ScheduleID"));

        Timestamp bookingDate = rs.getTimestamp("BookingDate");
        if (bookingDate != null) {
            booking.setBookingDate(bookingDate.toLocalDateTime());
        }

        booking.setTotalPrice(rs.getBigDecimal("TotalPrice"));

        int couponId = rs.getInt("CouponID");
        booking.setCouponId(rs.wasNull() ? null : couponId);

        booking.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
        booking.setSurchargeAmount(rs.getBigDecimal("SurchargeAmount"));
        booking.setFinalPrice(rs.getBigDecimal("FinalPrice"));

        String status = rs.getString("Status");
        if (status != null) {
            try {
                booking.setStatus(Status.valueOf(status));
            } catch (IllegalArgumentException e) {
                System.out.println("Status booking khong hop le trong database: " + status);
            }
        }

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            booking.setCreatedAt(createdAt.toLocalDateTime());
        }

        booking.setTourName(rs.getString("TourName"));
        booking.setUserFullName(decryptSafely(rs.getString("UserFullName")));

        return booking;
    }

    private BookingInput validateBookingInput(int userId,
                                              int tourId,
                                              int scheduleId,
                                              int adultCount,
                                              int childCount,
                                              int babyCount) {
        if (userId <= 0) {
            return BookingInput.invalid("UserID khong hop le.");
        }

        if (tourId <= 0) {
            return BookingInput.invalid("TourID khong hop le.");
        }

        if (scheduleId <= 0) {
            return BookingInput.invalid("ScheduleID khong hop le.");
        }

        if (adultCount < 1) {
            return BookingInput.invalid("Phai co it nhat 1 nguoi lon.");
        }

        if (childCount < 0 || babyCount < 0) {
            return BookingInput.invalid("So luong tre em / em be khong duoc am.");
        }

        if (adultCount + childCount > 100) {
            return BookingInput.invalid("So khach chiem ghe qua lon.");
        }

        return BookingInput.valid(userId, tourId, scheduleId, adultCount, childCount, babyCount);
    }

    private String normalizeCancelBy(String cancelBy) {
        String value = cleanString(cancelBy);

        if (value == null) {
            return null;
        }

        value = value.toUpperCase();

        if (CANCEL_BY_CUSTOMER.equals(value)) {
            return CANCEL_BY_CUSTOMER;
        }

        if (CANCEL_BY_COMPANY.equals(value)
                || "STAFF".equals(value)
                || "MANAGER".equals(value)
                || "SYSTEM".equals(value)) {
            return CANCEL_BY_COMPANY;
        }

        return null;
    }

    private BigDecimal normalizeRefundPercent(BigDecimal refundPercent) {
        if (refundPercent == null) {
            return BigDecimal.ZERO;
        }

        if (refundPercent.compareTo(BigDecimal.ZERO) < 0
                || refundPercent.compareTo(new BigDecimal("100")) > 0) {
            return null;
        }

        return refundPercent.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeBookingStatus(String status) {
        String value = cleanString(status);

        if (value == null) {
            return null;
        }

        value = value.toUpperCase();

        if (STATUS_PENDING.equals(value)
                || STATUS_PAID.equals(value)
                || STATUS_COMPLETED.equals(value)
                || STATUS_CANCELLED.equals(value)) {
            return value;
        }

        return null;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 20;
        }

        return Math.min(pageSize, 100);
    }

    private Integer getNullableInt(ResultSet rs, String columnLabel) throws SQLException {
        int value = rs.getInt(columnLabel);
        return rs.wasNull() ? null : value;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private String decryptSafely(String value) {
        if (value == null) {
            return null;
        }

        try {
            return AES256Util.decrypt(value);
        } catch (Exception e) {
            return value;
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

        if (errorCode == 50001) {
            System.out.println("Loi " + methodName + ": Khong du ghe trong.");
        } else if (errorCode == 50044) {
            System.out.println("Loi " + methodName + ": So tien hoan vuot qua FinalPrice.");
        } else if (errorCode == 50050 || errorCode == 50051 || errorCode == 50052) {
            System.out.println("Loi " + methodName + ": Khong tinh duoc gia tour theo procedure.");
        } else if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": UserID/TourID/ScheduleID/CouponID khong ton tai hoac vi pham khoa ngoai.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Du lieu bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang BOOKINGS/BOOKING_PASSENGERS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang lien quan.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class ScheduleSnapshot {
        private int availableSlots;
        private int bookedSlots;
        private BigDecimal basePrice;
        private BigDecimal priceMultiplier;
        private BigDecimal surcharge;
    }

    private static class PriceSnapshot {
        private final BigDecimal adultPrice;
        private final BigDecimal childPrice;
        private final BigDecimal babyPrice;

        private PriceSnapshot(BigDecimal adultPrice, BigDecimal childPrice, BigDecimal babyPrice) {
            this.adultPrice = adultPrice;
            this.childPrice = childPrice;
            this.babyPrice = babyPrice;
        }
    }

    private static class CouponInfo {
        private final int couponId;
        private final BigDecimal discountAmount;

        private CouponInfo(int couponId, BigDecimal discountAmount) {
            this.couponId = couponId;
            this.discountAmount = discountAmount;
        }
    }

    private static class BookingInput {
        private final boolean valid;
        private final String message;
        private final int userId;
        private final int tourId;
        private final int scheduleId;
        private final int adultCount;
        private final int childCount;
        private final int babyCount;

        private BookingInput(boolean valid,
                             String message,
                             int userId,
                             int tourId,
                             int scheduleId,
                             int adultCount,
                             int childCount,
                             int babyCount) {
            this.valid = valid;
            this.message = message;
            this.userId = userId;
            this.tourId = tourId;
            this.scheduleId = scheduleId;
            this.adultCount = adultCount;
            this.childCount = childCount;
            this.babyCount = babyCount;
        }

        private static BookingInput valid(int userId,
                                          int tourId,
                                          int scheduleId,
                                          int adultCount,
                                          int childCount,
                                          int babyCount) {
            return new BookingInput(
                    true,
                    null,
                    userId,
                    tourId,
                    scheduleId,
                    adultCount,
                    childCount,
                    babyCount
            );
        }

        private static BookingInput invalid(String message) {
            return new BookingInput(
                    false,
                    message,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }
    }
}
