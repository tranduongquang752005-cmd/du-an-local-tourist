package dao;

import config.DatabaseConnection;
import model.FeaturedTour;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class FeaturedTourDAO {

    private static final int MAX_FEATURED_TITLE_LENGTH = 200;
    private static final int MAX_FEATURED_DESCRIPTION_LENGTH = 1000;

    public int createFeaturedTour(int tourId,
                                  int displayOrder,
                                  String featuredTitle,
                                  String featuredDescription) {
        FeaturedTourInput input = validateFeaturedTourInput(
                tourId,
                displayOrder,
                featuredTitle,
                featuredDescription
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isActiveTourExists(input.tourId)) {
            System.out.println("TourID khong ton tai hoac dang bi tat.");
            return -1;
        }

        if (getFeaturedTourByTourId(input.tourId) != null) {
            System.out.println("Tour nay da nam trong featured list.");
            return -1;
        }

        if (isDisplayOrderExists(input.displayOrder, null)) {
            System.out.println("DisplayOrder da ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO FEATURED_TOURS
                (
                    TourID,
                    DisplayOrder,
                    FeaturedTitle,
                    FeaturedDescription,
                    IsActive
                )
                VALUES (?, ?, ?, ?, 1)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.tourId);
            ps.setInt(2, input.displayOrder);
            ps.setString(3, input.featuredTitle);
            ps.setString(4, input.featuredDescription);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createFeaturedTour");
        }

        return -1;
    }

    public boolean updateFeaturedTour(int featuredTourId,
                                      int displayOrder,
                                      String featuredTitle,
                                      String featuredDescription) {
        if (featuredTourId <= 0) {
            System.out.println("FeaturedTourID khong hop le.");
            return false;
        }

        FeaturedTour current = getFeaturedTourById(featuredTourId);

        if (current == null) {
            System.out.println("Khong tim thay featured tour.");
            return false;
        }

        FeaturedTourInput input = validateFeaturedTourInput(
                current.getTourId(),
                displayOrder,
                featuredTitle,
                featuredDescription
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        if (isDisplayOrderExists(input.displayOrder, featuredTourId)) {
            System.out.println("DisplayOrder da ton tai o featured tour khac.");
            return false;
        }

        String sql = """
                UPDATE FEATURED_TOURS
                SET DisplayOrder = ?,
                    FeaturedTitle = ?,
                    FeaturedDescription = ?
                WHERE FeaturedTourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, input.displayOrder);
            ps.setString(2, input.featuredTitle);
            ps.setString(3, input.featuredDescription);
            ps.setInt(4, featuredTourId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat featured tour that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateFeaturedTour");
        }

        return false;
    }

    public boolean activateFeaturedTour(int featuredTourId) {
        if (featuredTourId <= 0) {
            System.out.println("FeaturedTourID khong hop le.");
            return false;
        }

        FeaturedTour featuredTour = getFeaturedTourById(featuredTourId);

        if (featuredTour == null) {
            System.out.println("Khong tim thay featured tour.");
            return false;
        }

        if (!isActiveTourExists(featuredTour.getTourId())) {
            System.out.println("Tour goc dang bi tat, khong the bat featured.");
            return false;
        }

        return updateActiveStatus(featuredTourId, true);
    }

    public boolean deactivateFeaturedTour(int featuredTourId) {
        return updateActiveStatus(featuredTourId, false);
    }

    public boolean deleteFeaturedTour(int featuredTourId) {
        if (featuredTourId <= 0) {
            System.out.println("FeaturedTourID khong hop le.");
            return false;
        }

        String sql = """
                DELETE FROM FEATURED_TOURS
                WHERE FeaturedTourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, featuredTourId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay featured tour de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteFeaturedTour");
        }

        return false;
    }

    public FeaturedTour getFeaturedTourById(int featuredTourId) {
        if (featuredTourId <= 0) {
            System.out.println("FeaturedTourID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE FeaturedTourID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, featuredTourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFeaturedTour(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getFeaturedTourById");
        }

        return null;
    }

    public FeaturedTour getFeaturedTourByTourId(int tourId) {
        if (tourId <= 0) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFeaturedTour(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getFeaturedTourByTourId");
        }

        return null;
    }

    public List<FeaturedTour> getAllFeaturedTours() {
        String sql = buildSelectSql("""
                ORDER BY IsActive DESC, DisplayOrder ASC, FeaturedTourID ASC
                """);

        return queryFeaturedTourList(sql, null, "getAllFeaturedTours");
    }

    public List<FeaturedTour> getActiveFeaturedTours() {
        String sql = buildSelectSql("""
                WHERE IsActive = 1
                ORDER BY DisplayOrder ASC, FeaturedTourID ASC
                """);

        return queryFeaturedTourList(sql, null, "getActiveFeaturedTours");
    }

    public boolean updateDisplayOrder(int featuredTourId, int displayOrder) {
        if (featuredTourId <= 0) {
            System.out.println("FeaturedTourID khong hop le.");
            return false;
        }

        if (displayOrder <= 0 || displayOrder > 1000) {
            System.out.println("DisplayOrder phai tu 1 den 1000.");
            return false;
        }

        if (getFeaturedTourById(featuredTourId) == null) {
            System.out.println("Khong tim thay featured tour.");
            return false;
        }

        if (isDisplayOrderExists(displayOrder, featuredTourId)) {
            System.out.println("DisplayOrder da ton tai o featured tour khac.");
            return false;
        }

        String sql = """
                UPDATE FEATURED_TOURS
                SET DisplayOrder = ?
                WHERE FeaturedTourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, displayOrder);
            ps.setInt(2, featuredTourId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat DisplayOrder that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateDisplayOrder");
        }

        return false;
    }

    public int countActiveFeaturedTours() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM FEATURED_TOURS
                WHERE IsActive = 1
                """;

        return queryCount(sql, null, "countActiveFeaturedTours");
    }

    private boolean updateActiveStatus(int featuredTourId, boolean active) {
        if (featuredTourId <= 0) {
            System.out.println("FeaturedTourID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE FEATURED_TOURS
                SET IsActive = ?
                WHERE FeaturedTourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, featuredTourId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay featured tour.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateActiveStatus");
        }

        return false;
    }

    private boolean isActiveTourExists(int tourId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE TourID = ?
                  AND IsActive = 1
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, tourId),
                "isActiveTourExists"
        ) > 0;
    }

    private boolean isDisplayOrderExists(int displayOrder, Integer excludedFeaturedTourId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS Total
                FROM FEATURED_TOURS
                WHERE DisplayOrder = ?
                """);

        if (excludedFeaturedTourId != null) {
            sql.append(" AND FeaturedTourID <> ? ");
        }

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())
        ) {
            ps.setInt(1, displayOrder);

            if (excludedFeaturedTourId != null) {
                ps.setInt(2, excludedFeaturedTourId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("Total") > 0;
            }

        } catch (SQLException e) {
            handleException(e, "isDisplayOrderExists");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    FeaturedTourID,
                    TourID,
                    DisplayOrder,
                    FeaturedTitle,
                    FeaturedDescription,
                    IsActive,
                    CreatedAt
                FROM FEATURED_TOURS
                """ + condition;
    }

    private List<FeaturedTour> queryFeaturedTourList(String sql,
                                                     SqlSetter setter,
                                                     String methodName) {
        List<FeaturedTour> featuredTours = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    featuredTours.add(mapFeaturedTour(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return featuredTours;
    }

    private FeaturedTour mapFeaturedTour(ResultSet rs) throws SQLException {
        FeaturedTour featuredTour = new FeaturedTour();

        featuredTour.setFeaturedTourId(rs.getInt("FeaturedTourID"));
        featuredTour.setTourId(rs.getInt("TourID"));
        featuredTour.setDisplayOrder(rs.getInt("DisplayOrder"));
        featuredTour.setFeaturedTitle(rs.getString("FeaturedTitle"));
        featuredTour.setFeaturedDescription(rs.getString("FeaturedDescription"));
        featuredTour.setActive(rs.getBoolean("IsActive"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            featuredTour.setCreatedAt(createdAt.toLocalDateTime());
        }

        return featuredTour;
    }

    private FeaturedTourInput validateFeaturedTourInput(int tourId,
                                                        int displayOrder,
                                                        String featuredTitle,
                                                        String featuredDescription) {
        String cleanTitle = cleanString(featuredTitle);
        String cleanDescription = cleanString(featuredDescription);

        if (tourId <= 0) {
            return FeaturedTourInput.invalid("TourID khong hop le.");
        }

        if (displayOrder <= 0 || displayOrder > 1000) {
            return FeaturedTourInput.invalid("DisplayOrder phai tu 1 den 1000.");
        }

        if (cleanTitle != null && cleanTitle.length() > MAX_FEATURED_TITLE_LENGTH) {
            return FeaturedTourInput.invalid("FeaturedTitle qua dai, toi da 200 ky tu.");
        }

        if (cleanDescription != null && cleanDescription.length() > MAX_FEATURED_DESCRIPTION_LENGTH) {
            return FeaturedTourInput.invalid("FeaturedDescription qua dai, toi da 1000 ky tu.");
        }

        return FeaturedTourInput.valid(
                tourId,
                displayOrder,
                cleanTitle,
                cleanDescription
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
            System.out.println("Loi " + methodName + ": TourID hoac DisplayOrder da ton tai trong FEATURED_TOURS.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang FEATURED_TOURS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": DisplayOrder hoac du lieu featured khong hop le.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": FeaturedTitle/FeaturedDescription qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang FEATURED_TOURS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class FeaturedTourInput {
        private final boolean valid;
        private final String message;
        private final int tourId;
        private final int displayOrder;
        private final String featuredTitle;
        private final String featuredDescription;

        private FeaturedTourInput(boolean valid,
                                  String message,
                                  int tourId,
                                  int displayOrder,
                                  String featuredTitle,
                                  String featuredDescription) {
            this.valid = valid;
            this.message = message;
            this.tourId = tourId;
            this.displayOrder = displayOrder;
            this.featuredTitle = featuredTitle;
            this.featuredDescription = featuredDescription;
        }

        private static FeaturedTourInput valid(int tourId,
                                               int displayOrder,
                                               String featuredTitle,
                                               String featuredDescription) {
            return new FeaturedTourInput(
                    true,
                    null,
                    tourId,
                    displayOrder,
                    featuredTitle,
                    featuredDescription
            );
        }

        private static FeaturedTourInput invalid(String message) {
            return new FeaturedTourInput(
                    false,
                    message,
                    0,
                    0,
                    null,
                    null
            );
        }
    }
}
