package dao;

import config.DatabaseConnection;
import model.TourLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class TourLocationDAO {

    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    public int createTourLocation(int tourId,
                                  int locationId,
                                  int dayNumber,
                                  int sequenceOrder,
                                  String description) {
        TourLocationInput input = validateTourLocationInput(
                tourId,
                locationId,
                dayNumber,
                sequenceOrder,
                description
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isTourExists(input.tourId)) {
            System.out.println("TourID khong ton tai.");
            return -1;
        }

        if (!isLocationExists(input.locationId)) {
            System.out.println("LocationID khong ton tai.");
            return -1;
        }

        if (isDuplicateSequence(input.tourId, input.dayNumber, input.sequenceOrder, null)) {
            System.out.println("Tour da co location o DayNumber va SequenceOrder nay.");
            return -1;
        }

        String sql = """
                INSERT INTO TOUR_LOCATIONS
                (
                    TourID,
                    LocationID,
                    DayNumber,
                    SequenceOrder,
                    Description
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.tourId);
            ps.setInt(2, input.locationId);
            ps.setInt(3, input.dayNumber);
            ps.setInt(4, input.sequenceOrder);
            ps.setString(5, input.description);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createTourLocation");
        }

        return -1;
    }

    public boolean updateTourLocation(int tourLocationId,
                                      int locationId,
                                      int dayNumber,
                                      int sequenceOrder,
                                      String description) {
        if (tourLocationId <= 0) {
            System.out.println("TourLocationID khong hop le.");
            return false;
        }

        TourLocation current = getTourLocationById(tourLocationId);

        if (current == null) {
            System.out.println("Khong tim thay tour location.");
            return false;
        }

        TourLocationInput input = validateTourLocationInput(
                current.getTourId(),
                locationId,
                dayNumber,
                sequenceOrder,
                description
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        if (!isLocationExists(input.locationId)) {
            System.out.println("LocationID khong ton tai.");
            return false;
        }

        if (isDuplicateSequence(input.tourId, input.dayNumber, input.sequenceOrder, tourLocationId)) {
            System.out.println("Tour da co location khac o DayNumber va SequenceOrder nay.");
            return false;
        }

        String sql = """
                UPDATE TOUR_LOCATIONS
                SET LocationID = ?,
                    DayNumber = ?,
                    SequenceOrder = ?,
                    Description = ?
                WHERE TourLocationID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, input.locationId);
            ps.setInt(2, input.dayNumber);
            ps.setInt(3, input.sequenceOrder);
            ps.setString(4, input.description);
            ps.setInt(5, tourLocationId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat tour location that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateTourLocation");
        }

        return false;
    }

    public boolean deleteTourLocation(int tourLocationId) {
        if (tourLocationId <= 0) {
            System.out.println("TourLocationID khong hop le.");
            return false;
        }

        String sql = """
                DELETE FROM TOUR_LOCATIONS
                WHERE TourLocationID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourLocationId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay tour location de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteTourLocation");
        }

        return false;
    }

    public TourLocation getTourLocationById(int tourLocationId) {
        if (tourLocationId <= 0) {
            System.out.println("TourLocationID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE TourLocationID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourLocationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourLocation(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTourLocationById");
        }

        return null;
    }

    public List<TourLocation> getLocationsByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                ORDER BY DayNumber ASC, SequenceOrder ASC, TourLocationID ASC
                """);

        return queryTourLocationList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getLocationsByTourId"
        );
    }

    public List<TourLocation> getLocationsByTourIdAndDay(int tourId, int dayNumber) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        if (dayNumber <= 0) {
            System.out.println("DayNumber phai lon hon 0.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                  AND DayNumber = ?
                ORDER BY SequenceOrder ASC, TourLocationID ASC
                """);

        return queryTourLocationList(
                sql,
                ps -> {
                    ps.setInt(1, tourId);
                    ps.setInt(2, dayNumber);
                },
                "getLocationsByTourIdAndDay"
        );
    }

    public int countLocationsByTourId(int tourId) {
        if (tourId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOUR_LOCATIONS
                WHERE TourID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, tourId),
                "countLocationsByTourId"
        );
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    TourLocationID,
                    TourID,
                    LocationID,
                    DayNumber,
                    SequenceOrder,
                    Description,
                    CreatedAt
                FROM TOUR_LOCATIONS
                """ + condition;
    }

    private List<TourLocation> queryTourLocationList(String sql,
                                                     SqlSetter setter,
                                                     String methodName) {
        List<TourLocation> tourLocations = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tourLocations.add(mapTourLocation(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return tourLocations;
    }

    private TourLocation mapTourLocation(ResultSet rs) throws SQLException {
        TourLocation tourLocation = new TourLocation();

        tourLocation.setTourLocationId(rs.getInt("TourLocationID"));
        tourLocation.setTourId(rs.getInt("TourID"));
        tourLocation.setLocationId(rs.getInt("LocationID"));
        tourLocation.setDayNumber(rs.getInt("DayNumber"));
        tourLocation.setSequenceOrder(rs.getInt("SequenceOrder"));
        tourLocation.setDescription(rs.getString("Description"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            tourLocation.setCreatedAt(createdAt.toLocalDateTime());
        }

        return tourLocation;
    }

    private TourLocationInput validateTourLocationInput(int tourId,
                                                        int locationId,
                                                        int dayNumber,
                                                        int sequenceOrder,
                                                        String description) {
        String cleanDescription = cleanString(description);

        if (tourId <= 0) {
            return TourLocationInput.invalid("TourID khong hop le.");
        }

        if (locationId <= 0) {
            return TourLocationInput.invalid("LocationID khong hop le.");
        }

        if (dayNumber <= 0 || dayNumber > 30) {
            return TourLocationInput.invalid("DayNumber phai tu 1 den 30.");
        }

        if (sequenceOrder <= 0 || sequenceOrder > 100) {
            return TourLocationInput.invalid("SequenceOrder phai tu 1 den 100.");
        }

        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            return TourLocationInput.invalid("Description qua dai, toi da 1000 ky tu.");
        }

        return TourLocationInput.valid(
                tourId,
                locationId,
                dayNumber,
                sequenceOrder,
                cleanDescription
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

    private boolean isLocationExists(int locationId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM LOCATIONS
                WHERE LocationID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, locationId),
                "isLocationExists"
        ) > 0;
    }

    private boolean isDuplicateSequence(int tourId,
                                        int dayNumber,
                                        int sequenceOrder,
                                        Integer excludedTourLocationId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS Total
                FROM TOUR_LOCATIONS
                WHERE TourID = ?
                  AND DayNumber = ?
                  AND SequenceOrder = ?
                """);

        if (excludedTourLocationId != null) {
            sql.append(" AND TourLocationID <> ? ");
        }

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())
        ) {
            ps.setInt(1, tourId);
            ps.setInt(2, dayNumber);
            ps.setInt(3, sequenceOrder);

            if (excludedTourLocationId != null) {
                ps.setInt(4, excludedTourLocationId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("Total") > 0;
            }

        } catch (SQLException e) {
            handleException(e, "isDuplicateSequence");
        }

        return false;
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
            System.out.println("Loi " + methodName + ": TourID/LocationID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Thu tu location trong tour bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TOUR_LOCATIONS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": DayNumber/SequenceOrder khong hop le.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Description qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TOUR_LOCATIONS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class TourLocationInput {
        private final boolean valid;
        private final String message;
        private final int tourId;
        private final int locationId;
        private final int dayNumber;
        private final int sequenceOrder;
        private final String description;

        private TourLocationInput(boolean valid,
                                  String message,
                                  int tourId,
                                  int locationId,
                                  int dayNumber,
                                  int sequenceOrder,
                                  String description) {
            this.valid = valid;
            this.message = message;
            this.tourId = tourId;
            this.locationId = locationId;
            this.dayNumber = dayNumber;
            this.sequenceOrder = sequenceOrder;
            this.description = description;
        }

        private static TourLocationInput valid(int tourId,
                                               int locationId,
                                               int dayNumber,
                                               int sequenceOrder,
                                               String description) {
            return new TourLocationInput(
                    true,
                    null,
                    tourId,
                    locationId,
                    dayNumber,
                    sequenceOrder,
                    description
            );
        }

        private static TourLocationInput invalid(String message) {
            return new TourLocationInput(
                    false,
                    message,
                    0,
                    0,
                    0,
                    0,
                    null
            );
        }
    }
}
