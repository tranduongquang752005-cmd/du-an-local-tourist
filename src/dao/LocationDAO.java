package dao;

import config.DatabaseConnection;
import model.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class LocationDAO {

    private static final int MAX_LOCATION_NAME_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    public List<Location> getAllLocations() {
        String sql = buildSelectSql("""
                ORDER BY LocationName ASC, LocationID ASC
                """);

        return queryLocationList(sql, null, "getAllLocations");
    }

    public Location getLocationById(int locationId) {
        if (locationId <= 0) {
            System.out.println("LocationID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE LocationID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, locationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLocation(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getLocationById");
        }

        return null;
    }

    public Location getLocationByName(String locationName) {
        String cleanName = cleanString(locationName);

        if (cleanName == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE LocationName = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLocation(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getLocationByName");
        }

        return null;
    }

    public List<Location> searchLocations(String keyword) {
        String cleanKeyword = cleanString(keyword);

        if (cleanKeyword == null) {
            return getAllLocations();
        }

        String sql = buildSelectSql("""
                WHERE LocationName LIKE ?
                   OR DescriptionLocation LIKE ?
                ORDER BY LocationName ASC, LocationID ASC
                """);

        return queryLocationList(
                sql,
                ps -> {
                    String likeKeyword = "%" + cleanKeyword + "%";
                    ps.setString(1, likeKeyword);
                    ps.setString(2, likeKeyword);
                },
                "searchLocations"
        );
    }

    public int createLocation(String locationName, String descriptionLocation) {
        LocationInput input = validateLocationInput(locationName, descriptionLocation);

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (getLocationByName(input.locationName) != null) {
            System.out.println("LocationName da ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO LOCATIONS
                (
                    LocationName,
                    DescriptionLocation
                )
                VALUES (?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, input.locationName);
            ps.setString(2, input.descriptionLocation);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createLocation");
        }

        return -1;
    }

    public boolean updateLocation(int locationId,
                                  String locationName,
                                  String descriptionLocation) {
        if (locationId <= 0) {
            System.out.println("LocationID khong hop le.");
            return false;
        }

        if (getLocationById(locationId) == null) {
            System.out.println("Khong tim thay location.");
            return false;
        }

        LocationInput input = validateLocationInput(locationName, descriptionLocation);

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        Location duplicated = getLocationByName(input.locationName);

        if (duplicated != null && duplicated.getLocationId() != locationId) {
            System.out.println("LocationName da ton tai o location khac.");
            return false;
        }

        String sql = """
                UPDATE LOCATIONS
                SET LocationName = ?,
                    DescriptionLocation = ?
                WHERE LocationID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, input.locationName);
            ps.setString(2, input.descriptionLocation);
            ps.setInt(3, locationId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat location that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateLocation");
        }

        return false;
    }

    public boolean deleteLocation(int locationId) {
        if (locationId <= 0) {
            System.out.println("LocationID khong hop le.");
            return false;
        }

        if (isLocationUsedByTour(locationId)) {
            System.out.println("Location da duoc tour tham chieu, khong the xoa.");
            return false;
        }

        String sql = """
                DELETE FROM LOCATIONS
                WHERE LocationID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, locationId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay location de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteLocation");
        }

        return false;
    }

    public int countLocations() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM LOCATIONS
                """;

        return queryCount(sql, null, "countLocations");
    }

    private boolean isLocationUsedByTour(int locationId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE LocationID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, locationId),
                "isLocationUsedByTour"
        ) > 0;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    LocationID,
                    LocationName,
                    DescriptionLocation,
                    CreatedAt
                FROM LOCATIONS
                """ + condition;
    }

    private List<Location> queryLocationList(String sql,
                                             SqlSetter setter,
                                             String methodName) {
        List<Location> locations = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    locations.add(mapLocation(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return locations;
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

    private Location mapLocation(ResultSet rs) throws SQLException {
        Location location = new Location();

        location.setLocationId(rs.getInt("LocationID"));
        location.setLocationName(rs.getString("LocationName"));
        location.setDescriptionLocation(rs.getString("DescriptionLocation"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            location.setCreatedAt(createdAt.toLocalDateTime());
        }

        return location;
    }

    private LocationInput validateLocationInput(String locationName, String descriptionLocation) {
        String cleanName = cleanString(locationName);
        String cleanDescription = cleanString(descriptionLocation);

        if (cleanName == null || cleanName.length() > MAX_LOCATION_NAME_LENGTH) {
            return LocationInput.invalid("LocationName khong hop le, toi da 150 ky tu.");
        }

        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            return LocationInput.invalid("DescriptionLocation qua dai, toi da 1000 ky tu.");
        }

        return LocationInput.valid(cleanName, cleanDescription);
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
            System.out.println("Loi " + methodName + ": Location dang duoc tham chieu hoac vi pham khoa ngoai.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": LocationName bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang LOCATIONS.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang LOCATIONS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class LocationInput {
        private final boolean valid;
        private final String message;
        private final String locationName;
        private final String descriptionLocation;

        private LocationInput(boolean valid,
                              String message,
                              String locationName,
                              String descriptionLocation) {
            this.valid = valid;
            this.message = message;
            this.locationName = locationName;
            this.descriptionLocation = descriptionLocation;
        }

        private static LocationInput valid(String locationName, String descriptionLocation) {
            return new LocationInput(true, null, locationName, descriptionLocation);
        }

        private static LocationInput invalid(String message) {
            return new LocationInput(false, message, null, null);
        }
    }
}
