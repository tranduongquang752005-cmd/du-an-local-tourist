package dao;

import config.DatabaseConnection;
import model.FuelPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

public class FuelPriceDAO {

    public List<FuelPrice> getAllFuelPrices() {
        String sql = buildSelectSql("""
                ORDER BY EffectiveDate DESC, FuelPriceID DESC
                """);

        return queryFuelPriceList(sql, null, "getAllFuelPrices");
    }

    public FuelPrice getFuelPriceById(int fuelPriceId) {
        if (fuelPriceId <= 0) {
            System.out.println("FuelPriceID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE FuelPriceID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, fuelPriceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFuelPrice(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getFuelPriceById");
        }

        return null;
    }

    public FuelPrice getFuelPriceByEffectiveDate(LocalDate effectiveDate) {
        if (effectiveDate == null) {
            System.out.println("EffectiveDate khong duoc null.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE EffectiveDate = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setDate(1, Date.valueOf(effectiveDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFuelPrice(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getFuelPriceByEffectiveDate");
        }

        return null;
    }

    public FuelPrice getLatestFuelPrice() {
        String sql = """
                SELECT TOP 1
                    FuelPriceID,
                    Price,
                    EffectiveDate,
                    CreatedAt
                FROM FUEL_PRICES
                ORDER BY EffectiveDate DESC, FuelPriceID DESC
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            if (rs.next()) {
                return mapFuelPrice(rs);
            }

        } catch (SQLException e) {
            handleException(e, "getLatestFuelPrice");
        }

        return null;
    }

    public FuelPrice getLatestFuelPriceBeforeOrOn(LocalDate targetDate) {
        if (targetDate == null) {
            System.out.println("TargetDate khong duoc null.");
            return null;
        }

        String sql = """
                SELECT TOP 1
                    FuelPriceID,
                    Price,
                    EffectiveDate,
                    CreatedAt
                FROM FUEL_PRICES
                WHERE EffectiveDate <= ?
                ORDER BY EffectiveDate DESC, FuelPriceID DESC
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setDate(1, Date.valueOf(targetDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFuelPrice(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getLatestFuelPriceBeforeOrOn");
        }

        return null;
    }

    public List<FuelPrice> getFuelPricesByDateRange(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = buildSelectSql("""
                WHERE EffectiveDate BETWEEN ? AND ?
                ORDER BY EffectiveDate DESC, FuelPriceID DESC
                """);

        return queryFuelPriceList(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "getFuelPricesByDateRange"
        );
    }

    public int createFuelPrice(BigDecimal price, LocalDate effectiveDate) {
        FuelPriceInput input = validateFuelPriceInput(price, effectiveDate);

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (getFuelPriceByEffectiveDate(input.effectiveDate) != null) {
            System.out.println("Da ton tai gia xang cho EffectiveDate nay.");
            return -1;
        }

        String sql = """
                INSERT INTO FUEL_PRICES
                (
                    Price,
                    EffectiveDate
                )
                VALUES (?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setBigDecimal(1, input.price);
            ps.setDate(2, Date.valueOf(input.effectiveDate));

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createFuelPrice");
        }

        return -1;
    }

    public boolean updateFuelPrice(int fuelPriceId,
                                   BigDecimal price,
                                   LocalDate effectiveDate) {
        if (fuelPriceId <= 0) {
            System.out.println("FuelPriceID khong hop le.");
            return false;
        }

        FuelPrice current = getFuelPriceById(fuelPriceId);

        if (current == null) {
            System.out.println("Khong tim thay FuelPrice.");
            return false;
        }

        if (isUsedBySchedule(fuelPriceId)) {
            System.out.println("Khong nen sua gia xang da duoc gan vao lich khoi hanh.");
            System.out.println("Hay tao ban ghi gia xang moi de bao toan lich su tinh gia.");
            return false;
        }

        FuelPriceInput input = validateFuelPriceInput(price, effectiveDate);

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        FuelPrice duplicated = getFuelPriceByEffectiveDate(input.effectiveDate);

        if (duplicated != null && duplicated.getFuelPriceId() != fuelPriceId) {
            System.out.println("EffectiveDate nay da ton tai o FuelPrice khac.");
            return false;
        }

        String sql = """
                UPDATE FUEL_PRICES
                SET Price = ?,
                    EffectiveDate = ?
                WHERE FuelPriceID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBigDecimal(1, input.price);
            ps.setDate(2, Date.valueOf(input.effectiveDate));
            ps.setInt(3, fuelPriceId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat FuelPrice that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateFuelPrice");
        }

        return false;
    }

    public boolean deleteFuelPrice(int fuelPriceId) {
        if (fuelPriceId <= 0) {
            System.out.println("FuelPriceID khong hop le.");
            return false;
        }

        if (isUsedBySchedule(fuelPriceId)) {
            System.out.println("Khong the xoa FuelPrice da duoc gan vao TOUR_SCHEDULES.");
            return false;
        }

        String sql = """
                DELETE FROM FUEL_PRICES
                WHERE FuelPriceID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, fuelPriceId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay FuelPrice de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteFuelPrice");
        }

        return false;
    }

    public boolean isUsedBySchedule(int fuelPriceId) {
        if (fuelPriceId <= 0) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOUR_SCHEDULES
                WHERE FuelPriceID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, fuelPriceId),
                "isUsedBySchedule"
        ) > 0;
    }

    public int countFuelPrices() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM FUEL_PRICES
                """;

        return queryCount(sql, null, "countFuelPrices");
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    FuelPriceID,
                    Price,
                    EffectiveDate,
                    CreatedAt
                FROM FUEL_PRICES
                """ + condition;
    }

    private List<FuelPrice> queryFuelPriceList(String sql,
                                               SqlSetter setter,
                                               String methodName) {
        List<FuelPrice> fuelPrices = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fuelPrices.add(mapFuelPrice(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return fuelPrices;
    }

    private FuelPrice mapFuelPrice(ResultSet rs) throws SQLException {
        FuelPrice fuelPrice = new FuelPrice();

        fuelPrice.setFuelPriceId(rs.getInt("FuelPriceID"));
        fuelPrice.setPrice(rs.getBigDecimal("Price"));

        Date effectiveDate = rs.getDate("EffectiveDate");
        if (effectiveDate != null) {
            fuelPrice.setEffectiveDate(effectiveDate.toLocalDate());
        }

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            fuelPrice.setCreatedAt(createdAt.toLocalDateTime());
        }

        return fuelPrice;
    }

    private FuelPriceInput validateFuelPriceInput(BigDecimal price, LocalDate effectiveDate) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return FuelPriceInput.invalid("Price phai lon hon 0.");
        }

        if (effectiveDate == null) {
            return FuelPriceInput.invalid("EffectiveDate khong duoc null.");
        }

        return FuelPriceInput.valid(
                price.setScale(2, RoundingMode.HALF_UP),
                effectiveDate
        );
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

    private DateRange normalizeDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate validFromDate = fromDate == null ? LocalDate.of(2000, 1, 1) : fromDate;
        LocalDate validToDate = toDate == null ? LocalDate.now() : toDate;

        if (validFromDate.isAfter(validToDate)) {
            LocalDate temp = validFromDate;
            validFromDate = validToDate;
            validToDate = temp;
        }

        return new DateRange(validFromDate, validToDate);
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": FuelPrice dang duoc tham chieu hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": EffectiveDate da ton tai.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang FUEL_PRICES.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Price phai lon hon 0.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang FUEL_PRICES.");
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

    private static class FuelPriceInput {
        private final boolean valid;
        private final String message;
        private final BigDecimal price;
        private final LocalDate effectiveDate;

        private FuelPriceInput(boolean valid,
                               String message,
                               BigDecimal price,
                               LocalDate effectiveDate) {
            this.valid = valid;
            this.message = message;
            this.price = price;
            this.effectiveDate = effectiveDate;
        }

        private static FuelPriceInput valid(BigDecimal price, LocalDate effectiveDate) {
            return new FuelPriceInput(true, null, price, effectiveDate);
        }

        private static FuelPriceInput invalid(String message) {
            return new FuelPriceInput(false, message, null, null);
        }
    }
}
