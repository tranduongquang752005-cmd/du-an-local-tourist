package dao;

import config.DatabaseConnection;
import model.TourImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class TourImageDAO {

    public List<TourImage> getImagesByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                  AND IsActive = 1
                ORDER BY ImageID ASC
                """);

        return queryImageList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getImagesByTourId"
        );
    }

    public List<TourImage> getAllImagesByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TourID = ?
                ORDER BY IsActive DESC, ImageID ASC
                """);

        return queryImageList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getAllImagesByTourId"
        );
    }

    public TourImage getMainImageByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return null;
        }

        String sql = """
                SELECT TOP 1
                    ImageID,
                    TourID,
                    ImageURL,
                    IsActive,
                    CreatedAt
                FROM TOUR_IMAGES
                WHERE TourID = ?
                  AND IsActive = 1
                ORDER BY ImageID ASC
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourImage(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getMainImageByTourId");
        }

        return null;
    }

    public TourImage getImageById(int imageId) {
        if (imageId <= 0) {
            System.out.println("ImageID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE ImageID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, imageId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourImage(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getImageById");
        }

        return null;
    }

    public int createTourImage(int tourId, String imageUrl) {
        String cleanImageUrl = cleanString(imageUrl);

        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return -1;
        }

        if (!isValidImageUrl(cleanImageUrl)) {
            System.out.println("ImageURL khong hop le, toi da 500 ky tu.");
            return -1;
        }

        if (!isTourExists(tourId)) {
            System.out.println("TourID khong ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO TOUR_IMAGES
                (
                    TourID,
                    ImageURL,
                    IsActive
                )
                VALUES (?, ?, 1)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, tourId);
            ps.setString(2, cleanImageUrl);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createTourImage");
        }

        return -1;
    }

    public boolean updateImageUrl(int imageId, String imageUrl) {
        String cleanImageUrl = cleanString(imageUrl);

        if (imageId <= 0) {
            System.out.println("ImageID khong hop le.");
            return false;
        }

        if (!isValidImageUrl(cleanImageUrl)) {
            System.out.println("ImageURL khong hop le, toi da 500 ky tu.");
            return false;
        }

        if (getImageById(imageId) == null) {
            System.out.println("Khong tim thay image.");
            return false;
        }

        String sql = """
                UPDATE TOUR_IMAGES
                SET ImageURL = ?
                WHERE ImageID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanImageUrl);
            ps.setInt(2, imageId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "updateImageUrl");
        }

        return false;
    }

    public boolean activateImage(int imageId) {
        return updateImageActiveStatus(imageId, true);
    }

    public boolean deactivateImage(int imageId) {
        if (imageId <= 0) {
            System.out.println("ImageID khong hop le.");
            return false;
        }

        TourImage current = getImageById(imageId);

        if (current == null) {
            System.out.println("Khong tim thay image.");
            return false;
        }

        int activeCount = countActiveImagesByTourId(current.getTourId());

        if (current.isActive() && activeCount <= 1) {
            System.out.println("Tour nen co it nhat 1 anh active de hien thi tren trang chu.");
            return false;
        }

        return updateImageActiveStatus(imageId, false);
    }

    public boolean deleteImage(int imageId) {
        if (imageId <= 0) {
            System.out.println("ImageID khong hop le.");
            return false;
        }

        TourImage current = getImageById(imageId);

        if (current == null) {
            System.out.println("Khong tim thay image.");
            return false;
        }

        int activeCount = countActiveImagesByTourId(current.getTourId());

        if (current.isActive() && activeCount <= 1) {
            System.out.println("Khong nen xoa anh active cuoi cung cua tour.");
            return false;
        }

        String sql = """
                DELETE FROM TOUR_IMAGES
                WHERE ImageID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, imageId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Xoa image that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteImage");
        }

        return false;
    }

    public int countActiveImagesByTourId(int tourId) {
        if (tourId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOUR_IMAGES
                WHERE TourID = ?
                  AND IsActive = 1
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
            handleException(e, "countActiveImagesByTourId");
        }

        return 0;
    }

    private boolean updateImageActiveStatus(int imageId, boolean active) {
        if (imageId <= 0) {
            System.out.println("ImageID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE TOUR_IMAGES
                SET IsActive = ?
                WHERE ImageID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, imageId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay image.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateImageActiveStatus");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    ImageID,
                    TourID,
                    ImageURL,
                    IsActive,
                    CreatedAt
                FROM TOUR_IMAGES
                """ + condition;
    }

    private List<TourImage> queryImageList(String sql, SqlSetter setter, String methodName) {
        List<TourImage> images = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    images.add(mapTourImage(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return images;
    }

    private TourImage mapTourImage(ResultSet rs) throws SQLException {
        TourImage image = new TourImage();

        image.setImageId(rs.getInt("ImageID"));
        image.setTourId(rs.getInt("TourID"));
        image.setImageUrl(rs.getString("ImageURL"));
        image.setActive(rs.getBoolean("IsActive"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            image.setCreatedAt(createdAt.toLocalDateTime());
        }

        return image;
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

    private boolean isValidImageUrl(String imageUrl) {
        return imageUrl != null
                && !imageUrl.trim().isEmpty()
                && imageUrl.length() <= 500;
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
            System.out.println("Loi " + methodName + ": TourID khong ton tai hoac vi pham khoa ngoai.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TOUR_IMAGES.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": ImageURL qua dai so voi cot trong database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TOUR_IMAGES.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }
}
