package dao;

import config.DatabaseConnection;
import model.TourSchedule;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TourScheduleDAO {

    public List<TourSchedule> getSchedulesByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                ORDER BY ScheduleDate ASC, ScheduleID ASC
                """);

        return queryScheduleList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getSchedulesByTourId"
        );
    }

    public List<TourSchedule> getAvailableSchedulesByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                  AND ScheduleDate >= CAST(GETDATE() AS DATE)
                  AND BookedSlots < AvailableSlots
                ORDER BY ScheduleDate ASC, ScheduleID ASC
                """);

        return queryScheduleList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getAvailableSchedulesByTourId"
        );
    }

    public TourSchedule getScheduleById(int scheduleId) {
        if (scheduleId <= 0) {
            System.out.println("ScheduleID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE ScheduleID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, scheduleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourSchedule(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getScheduleById");
        }

        return null;
    }

    public TourSchedule getScheduleByTourAndDate(int tourId, LocalDate scheduleDate) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return null;
        }

        if (scheduleDate == null) {
            System.out.println("ScheduleDate khong duoc null.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                  AND ScheduleDate = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);
            ps.setDate(2, Date.valueOf(scheduleDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourSchedule(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getScheduleByTourAndDate");
        }

        return null;
    }

    public List<TourSchedule> getSchedulesByDateRange(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = buildSelectSql("""
                WHERE ScheduleDate BETWEEN ? AND ?
                ORDER BY ScheduleDate ASC, TourID ASC, ScheduleID ASC
                """);

        return queryScheduleList(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "getSchedulesByDateRange"
        );
    }

    public boolean hasEnoughSlots(int scheduleId, int adultCount, int childCount) {
        if (scheduleId <= 0) {
            return false;
        }

        if (adultCount < 0 || childCount < 0) {
            return false;
        }

        int neededSlots = adultCount + childCount;

        if (neededSlots <= 0) {
            return false;
        }

        TourSchedule schedule = getScheduleById(scheduleId);

        if (schedule == null) {
            return false;
        }

        int remainingSlots = schedule.getAvailableSlots() - schedule.getBookedSlots();
        return remainingSlots >= neededSlots;
    }

    public int createSchedule(int tourId,
                              LocalDate scheduleDate,
                              int availableSlots,
                              BigDecimal priceMultiplier,
                              BigDecimal surcharge,
                              Integer fuelPriceId) {
        ScheduleInput input = validateInput(
                tourId,
                scheduleDate,
                availableSlots,
                priceMultiplier,
                surcharge,
                fuelPriceId
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isTourExists(input.tourId)) {
            System.out.println("TourID khong ton tai.");
            return -1;
        }

        if (input.fuelPriceId != null && !isFuelPriceExists(input.fuelPriceId)) {
            System.out.println("FuelPriceID khong ton tai.");
            return -1;
        }

        if (getScheduleByTourAndDate(input.tourId, input.scheduleDate) != null) {
            System.out.println("Tour da co lich khoi hanh trong ngay nay.");
            return -1;
        }

        String sql = """
                INSERT INTO TOUR_SCHEDULES
                (
                    TourID,
                    ScheduleDate,
                    AvailableSlots,
                    BookedSlots,
                    PriceMultiplier,
                    Surcharge,
                    FuelPriceID
                )
                VALUES (?, ?, ?, 0, ?, ?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.tourId);
            ps.setDate(2, Date.valueOf(input.scheduleDate));
            ps.setInt(3, input.availableSlots);
            ps.setBigDecimal(4, input.priceMultiplier);
            ps.setBigDecimal(5, input.surcharge);
            setNullableInt(ps, 6, input.fuelPriceId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createSchedule");
        }

        return -1;
    }

    public boolean updateSchedule(int scheduleId,
                                  LocalDate scheduleDate,
                                  int availableSlots,
                                  BigDecimal priceMultiplier,
                                  BigDecimal surcharge,
                                  Integer fuelPriceId) {
        if (scheduleId <= 0) {
            System.out.println("ScheduleID khong hop le.");
            return false;
        }

        TourSchedule current = getScheduleById(scheduleId);

        if (current == null) {
            System.out.println("Khong tim thay schedule.");
            return false;
        }

        if (current.getBookedSlots() > availableSlots) {
            System.out.println("AvailableSlots khong duoc nho hon BookedSlots hien tai.");
            return false;
        }

        ScheduleInput input = validateInput(
                current.getTourId(),
                scheduleDate,
                availableSlots,
                priceMultiplier,
                surcharge,
                fuelPriceId
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        if (input.fuelPriceId != null && !isFuelPriceExists(input.fuelPriceId)) {
            System.out.println("FuelPriceID khong ton tai.");
            return false;
        }

        TourSchedule duplicated = getScheduleByTourAndDate(current.getTourId(), input.scheduleDate);

        if (duplicated != null && duplicated.getScheduleId() != scheduleId) {
            System.out.println("Tour da co lich khoi hanh khac trong ngay nay.");
            return false;
        }

        String sql = """
                UPDATE TOUR_SCHEDULES
                SET ScheduleDate = ?,
                    AvailableSlots = ?,
                    PriceMultiplier = ?,
                    Surcharge = ?,
                    FuelPriceID = ?
                WHERE ScheduleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setDate(1, Date.valueOf(input.scheduleDate));
            ps.setInt(2, input.availableSlots);
            ps.setBigDecimal(3, input.priceMultiplier);
            ps.setBigDecimal(4, input.surcharge);
            setNullableInt(ps, 5, input.fuelPriceId);
            ps.setInt(6, scheduleId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat schedule that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateSchedule");
        }

        return false;
    }

    public boolean updateAvailableSlots(int scheduleId, int availableSlots) {
        if (scheduleId <= 0) {
            System.out.println("ScheduleID khong hop le.");
            return false;
        }

        if (availableSlots <= 0) {
            System.out.println("AvailableSlots phai lon hon 0.");
            return false;
        }

        TourSchedule current = getScheduleById(scheduleId);

        if (current == null) {
            System.out.println("Khong tim thay schedule.");
            return false;
        }

        if (availableSlots < current.getBookedSlots()) {
            System.out.println("AvailableSlots khong duoc nho hon BookedSlots hien tai.");
            return false;
        }

        String sql = """
                UPDATE TOUR_SCHEDULES
                SET AvailableSlots = ?
                WHERE ScheduleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, availableSlots);
            ps.setInt(2, scheduleId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "updateAvailableSlots");
        }

        return false;
    }

    public boolean deleteSchedule(int scheduleId) {
        if (scheduleId <= 0) {
            System.out.println("ScheduleID khong hop le.");
            return false;
        }

        TourSchedule current = getScheduleById(scheduleId);

        if (current == null) {
            System.out.println("Khong tim thay schedule.");
            return false;
        }

        if (current.getBookedSlots() > 0) {
            System.out.println("Khong the xoa schedule da co booking/passenger.");
            return false;
        }

        if (isScheduleUsedByBooking(scheduleId)) {
            System.out.println("Khong the xoa schedule dang duoc booking tham chieu.");
            return false;
        }

        String sql = """
                DELETE FROM TOUR_SCHEDULES
                WHERE ScheduleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, scheduleId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Xoa schedule that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteSchedule");
        }

        return false;
    }

    public int countSchedulesByTourId(int tourId) {
        if (tourId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOUR_SCHEDULES
                WHERE TourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total");
                }
            }

        } catch (SQLException e) {
            handleException(e, "countSchedulesByTourId");
        }

        return 0;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    ScheduleID,
                    TourID,
                    ScheduleDate,
                    AvailableSlots,
                    BookedSlots,
                    PriceMultiplier,
                    Surcharge,
                    FuelPriceID,
                    CreatedAt,
                    AvailableSlots - BookedSlots AS RemainingSlots
                FROM TOUR_SCHEDULES
                """ + condition;
    }

    private List<TourSchedule> queryScheduleList(String sql,
                                                 SqlSetter setter,
                                                 String methodName) {
        List<TourSchedule> schedules = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapTourSchedule(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return schedules;
    }

    private TourSchedule mapTourSchedule(ResultSet rs) throws SQLException {
        TourSchedule schedule = new TourSchedule();

        schedule.setScheduleId(rs.getInt("ScheduleID"));
        schedule.setTourId(rs.getInt("TourID"));

        Date scheduleDate = rs.getDate("ScheduleDate");
        if (scheduleDate != null) {
            schedule.setScheduleDate(scheduleDate.toLocalDate());
        }

        schedule.setAvailableSlots(rs.getInt("AvailableSlots"));
        schedule.setBookedSlots(rs.getInt("BookedSlots"));
        schedule.setPriceMultiplier(rs.getBigDecimal("PriceMultiplier"));
        schedule.setSurcharge(rs.getBigDecimal("Surcharge"));

        int fuelPriceId = rs.getInt("FuelPriceID");
        schedule.setFuelPriceId(rs.wasNull() ? null : fuelPriceId);

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            schedule.setCreatedAt(createdAt.toLocalDateTime());
        }

        // Model TourSchedule hiện tại không có setRemainingSlots().
        // RemainingSlots là cột tính nhanh trong SQL, khi cần thì tính bằng AvailableSlots - BookedSlots.

        return schedule;
    }

    private ScheduleInput validateInput(int tourId,
                                        LocalDate scheduleDate,
                                        int availableSlots,
                                        BigDecimal priceMultiplier,
                                        BigDecimal surcharge,
                                        Integer fuelPriceId) {
        if (tourId <= 0) {
            return ScheduleInput.invalid("TourID khong hop le.");
        }

        if (scheduleDate == null) {
            return ScheduleInput.invalid("ScheduleDate khong duoc null.");
        }

        if (availableSlots <= 0) {
            return ScheduleInput.invalid("AvailableSlots phai lon hon 0.");
        }

        if (priceMultiplier == null || priceMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            return ScheduleInput.invalid("PriceMultiplier phai lon hon 0.");
        }

        if (priceMultiplier.compareTo(new BigDecimal("10")) > 0) {
            return ScheduleInput.invalid("PriceMultiplier qua lon, nen <= 10.");
        }

        if (surcharge == null) {
            return ScheduleInput.invalid("Surcharge khong duoc null.");
        }

        if (surcharge.compareTo(BigDecimal.ZERO) < 0) {
            return ScheduleInput.invalid("Surcharge khong duoc am.");
        }

        if (fuelPriceId != null && fuelPriceId <= 0) {
            return ScheduleInput.invalid("FuelPriceID khong hop le.");
        }

        return ScheduleInput.valid(
                tourId,
                scheduleDate,
                availableSlots,
                priceMultiplier,
                surcharge,
                fuelPriceId
        );
    }

    private boolean isTourExists(int tourId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE TourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total") > 0;
                }
            }

        } catch (SQLException e) {
            handleException(e, "isTourExists");
        }

        return false;
    }

    private boolean isFuelPriceExists(int fuelPriceId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM FUEL_PRICES
                WHERE FuelPriceID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, fuelPriceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total") > 0;
                }
            }

        } catch (SQLException e) {
            handleException(e, "isFuelPriceExists");
        }

        return false;
    }

    private boolean isScheduleUsedByBooking(int scheduleId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                WHERE ScheduleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, scheduleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total") > 0;
                }
            }

        } catch (SQLException e) {
            handleException(e, "isScheduleUsedByBooking");
        }

        return true;
    }

    private DateRange normalizeDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate validFromDate = fromDate == null ? LocalDate.of(2000, 1, 1) : fromDate;
        LocalDate validToDate = toDate == null ? LocalDate.of(2100, 12, 31) : toDate;

        if (validFromDate.isAfter(validToDate)) {
            LocalDate temp = validFromDate;
            validFromDate = validToDate;
            validToDate = temp;
        }

        return new DateRange(validFromDate, validToDate);
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": TourID/FuelPriceID khong ton tai hoac schedule dang duoc tham chieu.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Lich khoi hanh bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TOUR_SCHEDULES.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TOUR_SCHEDULES.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class DateRange {
        private final LocalDate fromDate;
        private final LocalDate toDate;

        private DateRange(LocalDate fromDate, LocalDate toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }
    }

    private static class ScheduleInput {
        private final boolean valid;
        private final String message;
        private final int tourId;
        private final LocalDate scheduleDate;
        private final int availableSlots;
        private final BigDecimal priceMultiplier;
        private final BigDecimal surcharge;
        private final Integer fuelPriceId;

        private ScheduleInput(boolean valid,
                              String message,
                              int tourId,
                              LocalDate scheduleDate,
                              int availableSlots,
                              BigDecimal priceMultiplier,
                              BigDecimal surcharge,
                              Integer fuelPriceId) {
            this.valid = valid;
            this.message = message;
            this.tourId = tourId;
            this.scheduleDate = scheduleDate;
            this.availableSlots = availableSlots;
            this.priceMultiplier = priceMultiplier;
            this.surcharge = surcharge;
            this.fuelPriceId = fuelPriceId;
        }

        private static ScheduleInput valid(int tourId,
                                           LocalDate scheduleDate,
                                           int availableSlots,
                                           BigDecimal priceMultiplier,
                                           BigDecimal surcharge,
                                           Integer fuelPriceId) {
            return new ScheduleInput(
                    true,
                    null,
                    tourId,
                    scheduleDate,
                    availableSlots,
                    priceMultiplier,
                    surcharge,
                    fuelPriceId
            );
        }

        private static ScheduleInput invalid(String message) {
            return new ScheduleInput(
                    false,
                    message,
                    0,
                    null,
                    0,
                    null,
                    null,
                    null
            );
        }
    }
}
