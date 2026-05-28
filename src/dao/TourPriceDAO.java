package dao;

import config.DatabaseConnection;
import model.TourPrice;

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

public class TourPriceDAO {

    private static final int MAX_REASON_LENGTH = 500;

    public int createTourPrice(int tourId,
                               LocalDate effectiveDate,
                               BigDecimal price,
                               String reason) {
        TourPriceInput input = validateTourPriceInput(
                tourId,
                effectiveDate,
                price,
                reason
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isTourExists(input.tourId)) {
            System.out.println("TourID khong ton tai.");
            return -1;
        }

        if (getTourPriceByTourAndDate(input.tourId, input.effectiveDate) != null) {
            System.out.println("Tour da co gia ap dung cho EffectiveDate nay.");
            return -1;
        }

        String sql = """
                INSERT INTO TOUR_PRICES
                (
                    TourID,
                    EffectiveDate,
                    Price,
                    Reason
                )
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.tourId);
            ps.setDate(2, Date.valueOf(input.effectiveDate));
            ps.setBigDecimal(3, input.price);
            ps.setString(4, input.reason);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createTourPrice");
        }

        return -1;
    }

    public boolean updateTourPrice(int priceId,
                                   LocalDate effectiveDate,
                                   BigDecimal price,
                                   String reason) {
        if (priceId <= 0) {
            System.out.println("PriceID khong hop le.");
            return false;
        }

        TourPrice current = getTourPriceById(priceId);

        if (current == null) {
            System.out.println("Khong tim thay tour price.");
            return false;
        }

        TourPriceInput input = validateTourPriceInput(
                current.getTourId(),
                effectiveDate,
                price,
                reason
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        TourPrice duplicated = getTourPriceByTourAndDate(input.tourId, input.effectiveDate);

        if (duplicated != null && duplicated.getPriceId() != priceId) {
            System.out.println("Tour da co gia khac o EffectiveDate nay.");
            return false;
        }

        String sql = """
                UPDATE TOUR_PRICES
                SET EffectiveDate = ?,
                    Price = ?,
                    Reason = ?
                WHERE PriceID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setDate(1, Date.valueOf(input.effectiveDate));
            ps.setBigDecimal(2, input.price);
            ps.setString(3, input.reason);
            ps.setInt(4, priceId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat tour price that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateTourPrice");
        }

        return false;
    }

    public boolean deleteTourPrice(int priceId) {
        if (priceId <= 0) {
            System.out.println("PriceID khong hop le.");
            return false;
        }

        String sql = """
                DELETE FROM TOUR_PRICES
                WHERE PriceID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, priceId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay tour price de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteTourPrice");
        }

        return false;
    }

    public TourPrice getTourPriceById(int priceId) {
        if (priceId <= 0) {
            System.out.println("PriceID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE PriceID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, priceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourPrice(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTourPriceById");
        }

        return null;
    }

    public TourPrice getTourPriceByTourAndDate(int tourId, LocalDate effectiveDate) {
        if (tourId <= 0 || effectiveDate == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                  AND EffectiveDate = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);
            ps.setDate(2, Date.valueOf(effectiveDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourPrice(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTourPriceByTourAndDate");
        }

        return null;
    }

    public TourPrice getCurrentPriceByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return null;
        }

        String sql = """
                SELECT TOP 1
                    PriceID,
                    TourID,
                    EffectiveDate,
                    Price,
                    Reason,
                    CreatedAt
                FROM TOUR_PRICES
                WHERE TourID = ?
                  AND EffectiveDate <= CAST(GETDATE() AS DATE)
                ORDER BY EffectiveDate DESC, PriceID DESC
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourPrice(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getCurrentPriceByTourId");
        }

        return null;
    }

    public BigDecimal getCurrentPriceValueByTourId(int tourId) {
        TourPrice tourPrice = getCurrentPriceByTourId(tourId);

        if (tourPrice != null && tourPrice.getPrice() != null) {
            return tourPrice.getPrice();
        }

        return getTourBasePrice(tourId);
    }

    public List<TourPrice> getPricesByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                ORDER BY EffectiveDate DESC, PriceID DESC
                """);

        return queryTourPriceList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getPricesByTourId"
        );
    }

    public List<TourPrice> getPricesByEffectiveDateRange(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = buildSelectSql("""
                WHERE EffectiveDate BETWEEN ? AND ?
                ORDER BY EffectiveDate DESC, TourID ASC, PriceID DESC
                """);

        return queryTourPriceList(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "getPricesByEffectiveDateRange"
        );
    }

    public int countPricesByTourId(int tourId) {
        if (tourId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOUR_PRICES
                WHERE TourID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, tourId),
                "countPricesByTourId"
        );
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    PriceID,
                    TourID,
                    EffectiveDate,
                    Price,
                    Reason,
                    CreatedAt
                FROM TOUR_PRICES
                """ + condition;
    }

    private List<TourPrice> queryTourPriceList(String sql,
                                               SqlSetter setter,
                                               String methodName) {
        List<TourPrice> prices = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prices.add(mapTourPrice(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return prices;
    }

    private TourPrice mapTourPrice(ResultSet rs) throws SQLException {
        TourPrice tourPrice = new TourPrice();

        tourPrice.setPriceId(rs.getInt("PriceID"));
        tourPrice.setTourId(rs.getInt("TourID"));

        Date effectiveDate = rs.getDate("EffectiveDate");
        if (effectiveDate != null) {
            tourPrice.setEffectiveDate(effectiveDate.toLocalDate());
        }

        tourPrice.setPrice(rs.getBigDecimal("Price"));
        tourPrice.setReason(rs.getString("Reason"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            tourPrice.setCreatedAt(createdAt.toLocalDateTime());
        }

        return tourPrice;
    }

    private TourPriceInput validateTourPriceInput(int tourId,
                                                  LocalDate effectiveDate,
                                                  BigDecimal price,
                                                  String reason) {
        String cleanReason = cleanString(reason);

        if (tourId <= 0) {
            return TourPriceInput.invalid("TourID khong hop le.");
        }

        if (effectiveDate == null) {
            return TourPriceInput.invalid("EffectiveDate khong duoc null.");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return TourPriceInput.invalid("Price phai lon hon 0.");
        }

        if (cleanReason != null && cleanReason.length() > MAX_REASON_LENGTH) {
            return TourPriceInput.invalid("Reason qua dai, toi da 500 ky tu.");
        }

        return TourPriceInput.valid(
                tourId,
                effectiveDate,
                price.setScale(2, RoundingMode.HALF_UP),
                cleanReason
        );
    }

    private boolean isTourExists(int tourId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE TourID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, tourId),
                "isTourExists"
        ) > 0;
    }

    private BigDecimal getTourBasePrice(int tourId) {
        if (tourId <= 0) {
            return BigDecimal.ZERO;
        }

        String sql = """
                SELECT BasePrice
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
                    BigDecimal basePrice = rs.getBigDecimal("BasePrice");
                    return basePrice == null ? BigDecimal.ZERO : basePrice;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTourBasePrice");
        }

        return BigDecimal.ZERO;
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
            System.out.println("Loi " + methodName + ": TourID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Tour da co gia o EffectiveDate nay.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TOUR_PRICES.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Price hoac EffectiveDate khong hop le.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Reason qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TOUR_PRICES.");
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

    private static class TourPriceInput {
        private final boolean valid;
        private final String message;
        private final int tourId;
        private final LocalDate effectiveDate;
        private final BigDecimal price;
        private final String reason;

        private TourPriceInput(boolean valid,
                               String message,
                               int tourId,
                               LocalDate effectiveDate,
                               BigDecimal price,
                               String reason) {
            this.valid = valid;
            this.message = message;
            this.tourId = tourId;
            this.effectiveDate = effectiveDate;
            this.price = price;
            this.reason = reason;
        }

        private static TourPriceInput valid(int tourId,
                                            LocalDate effectiveDate,
                                            BigDecimal price,
                                            String reason) {
            return new TourPriceInput(
                    true,
                    null,
                    tourId,
                    effectiveDate,
                    price,
                    reason
            );
        }

        private static TourPriceInput invalid(String message) {
            return new TourPriceInput(
                    false,
                    message,
                    0,
                    null,
                    null,
                    null
            );
        }
    }
}
