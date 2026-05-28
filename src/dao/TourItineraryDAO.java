package dao;

import config.DatabaseConnection;
import model.TourItinerary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TourItineraryDAO {

    public List<TourItinerary> getItineraryByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                ORDER BY DayNumber ASC, TimeStart ASC, ItineraryID ASC
                """);

        return queryItineraryList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getItineraryByTourId"
        );
    }

    public List<TourItinerary> getItineraryByTourIdAndDay(int tourId, int dayNumber) {
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
                ORDER BY TimeStart ASC, ItineraryID ASC
                """);

        return queryItineraryList(
                sql,
                ps -> {
                    ps.setInt(1, tourId);
                    ps.setInt(2, dayNumber);
                },
                "getItineraryByTourIdAndDay"
        );
    }

    public TourItinerary getItineraryById(int itineraryId) {
        if (itineraryId <= 0) {
            System.out.println("ItineraryID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE ItineraryID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, itineraryId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourItinerary(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getItineraryById");
        }

        return null;
    }

    public int createItinerary(int tourId,
                               int dayNumber,
                               LocalTime timeStart,
                               LocalTime timeEnd,
                               String activity,
                               String description) {
        ItineraryInput input = validateInput(
                tourId,
                dayNumber,
                timeStart,
                timeEnd,
                activity,
                description
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isTourExists(tourId)) {
            System.out.println("TourID khong ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO TOUR_ITINERARY
                (
                    TourID,
                    DayNumber,
                    TimeStart,
                    TimeEnd,
                    Activity,
                    Description
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.tourId);
            ps.setInt(2, input.dayNumber);
            setNullableTime(ps, 3, input.timeStart);
            setNullableTime(ps, 4, input.timeEnd);
            ps.setString(5, input.activity);
            ps.setString(6, input.description);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createItinerary");
        }

        return -1;
    }

    public boolean updateItinerary(int itineraryId,
                                   int dayNumber,
                                   LocalTime timeStart,
                                   LocalTime timeEnd,
                                   String activity,
                                   String description) {
        if (itineraryId <= 0) {
            System.out.println("ItineraryID khong hop le.");
            return false;
        }

        TourItinerary current = getItineraryById(itineraryId);

        if (current == null) {
            System.out.println("Khong tim thay itinerary.");
            return false;
        }

        ItineraryInput input = validateInput(
                current.getTourId(),
                dayNumber,
                timeStart,
                timeEnd,
                activity,
                description
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        String sql = """
                UPDATE TOUR_ITINERARY
                SET DayNumber = ?,
                    TimeStart = ?,
                    TimeEnd = ?,
                    Activity = ?,
                    Description = ?
                WHERE ItineraryID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, input.dayNumber);
            setNullableTime(ps, 2, input.timeStart);
            setNullableTime(ps, 3, input.timeEnd);
            ps.setString(4, input.activity);
            ps.setString(5, input.description);
            ps.setInt(6, itineraryId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat itinerary that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateItinerary");
        }

        return false;
    }

    public boolean deleteItinerary(int itineraryId) {
        if (itineraryId <= 0) {
            System.out.println("ItineraryID khong hop le.");
            return false;
        }

        String sql = """
                DELETE FROM TOUR_ITINERARY
                WHERE ItineraryID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, itineraryId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay itinerary de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteItinerary");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    ItineraryID,
                    TourID,
                    DayNumber,
                    TimeStart,
                    TimeEnd,
                    Activity,
                    Description,
                    CreatedAt
                FROM TOUR_ITINERARY
                """ + condition;
    }

    private List<TourItinerary> queryItineraryList(String sql,
                                                   SqlSetter setter,
                                                   String methodName) {
        List<TourItinerary> itineraries = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    itineraries.add(mapTourItinerary(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return itineraries;
    }

    private TourItinerary mapTourItinerary(ResultSet rs) throws SQLException {
        TourItinerary itinerary = new TourItinerary();

        itinerary.setItineraryId(rs.getInt("ItineraryID"));
        itinerary.setTourId(rs.getInt("TourID"));
        itinerary.setDayNumber(rs.getInt("DayNumber"));

        Time timeStart = rs.getTime("TimeStart");
        if (timeStart != null) {
            itinerary.setTimeStart(timeStart.toLocalTime());
        }

        Time timeEnd = rs.getTime("TimeEnd");
        if (timeEnd != null) {
            itinerary.setTimeEnd(timeEnd.toLocalTime());
        }

        itinerary.setActivity(rs.getString("Activity"));
        itinerary.setDescription(rs.getString("Description"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            itinerary.setCreatedAt(createdAt.toLocalDateTime());
        }

        return itinerary;
    }

    private ItineraryInput validateInput(int tourId,
                                         int dayNumber,
                                         LocalTime timeStart,
                                         LocalTime timeEnd,
                                         String activity,
                                         String description) {
        String cleanActivity = cleanString(activity);
        String cleanDescription = cleanString(description);

        if (tourId <= 0) {
            return ItineraryInput.invalid("TourID khong hop le.");
        }

        if (dayNumber <= 0 || dayNumber > 30) {
            return ItineraryInput.invalid("DayNumber phai tu 1 den 30.");
        }

        if (timeStart != null && timeEnd != null && !timeStart.isBefore(timeEnd)) {
            return ItineraryInput.invalid("TimeStart phai nho hon TimeEnd.");
        }

        if (cleanActivity == null || cleanActivity.length() > 100) {
            return ItineraryInput.invalid("Activity khong hop le, toi da 100 ky tu.");
        }

        if (cleanDescription != null && cleanDescription.length() > 1000) {
            return ItineraryInput.invalid("Description qua dai, toi da 1000 ky tu.");
        }

        return ItineraryInput.valid(
                tourId,
                dayNumber,
                timeStart,
                timeEnd,
                cleanActivity,
                cleanDescription
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

    private void setNullableTime(PreparedStatement ps, int index, LocalTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.TIME);
        } else {
            ps.setTime(index, Time.valueOf(value));
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
            System.out.println("Loi " + methodName + ": TourID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TOUR_ITINERARY.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot trong database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TOUR_ITINERARY.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class ItineraryInput {
        private final boolean valid;
        private final String message;
        private final int tourId;
        private final int dayNumber;
        private final LocalTime timeStart;
        private final LocalTime timeEnd;
        private final String activity;
        private final String description;

        private ItineraryInput(boolean valid,
                               String message,
                               int tourId,
                               int dayNumber,
                               LocalTime timeStart,
                               LocalTime timeEnd,
                               String activity,
                               String description) {
            this.valid = valid;
            this.message = message;
            this.tourId = tourId;
            this.dayNumber = dayNumber;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.activity = activity;
            this.description = description;
        }

        private static ItineraryInput valid(int tourId,
                                            int dayNumber,
                                            LocalTime timeStart,
                                            LocalTime timeEnd,
                                            String activity,
                                            String description) {
            return new ItineraryInput(
                    true,
                    null,
                    tourId,
                    dayNumber,
                    timeStart,
                    timeEnd,
                    activity,
                    description
            );
        }

        private static ItineraryInput invalid(String message) {
            return new ItineraryInput(
                    false,
                    message,
                    0,
                    0,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
