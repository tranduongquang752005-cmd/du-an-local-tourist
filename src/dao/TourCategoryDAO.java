package dao;

import config.DatabaseConnection;
import model.TourCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class TourCategoryDAO {

    public List<TourCategory> getAllCategories() {
        String sql = """
                SELECT
                    CategoryID,
                    CategoryName,
                    Description
                FROM TOUR_CATEGORIES
                ORDER BY CategoryID ASC
                """;

        return queryCategoryList(sql, null, "getAllCategories");
    }

    public List<TourCategory> getActiveCategories() {
        /*
         * Bảng TOUR_CATEGORIES của bạn không có cột IsActive.
         * Vì vậy active categories = toàn bộ categories.
         */
        return getAllCategories();
    }

    public TourCategory getCategoryById(int categoryId) {
        if (categoryId <= 0) {
            System.out.println("CategoryID khong hop le.");
            return null;
        }

        String sql = """
                SELECT
                    CategoryID,
                    CategoryName,
                    Description
                FROM TOUR_CATEGORIES
                WHERE CategoryID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourCategory(rs);
                }
            }
        } catch (SQLException e) {
            handleException(e, "getCategoryById");
        }

        return null;
    }

    public TourCategory getCategoryByName(String categoryName) {
        String cleanName = cleanString(categoryName);

        if (cleanName == null) {
            System.out.println("CategoryName khong hop le.");
            return null;
        }

        String sql = """
                SELECT
                    CategoryID,
                    CategoryName,
                    Description
                FROM TOUR_CATEGORIES
                WHERE CategoryName = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTourCategory(rs);
                }
            }
        } catch (SQLException e) {
            handleException(e, "getCategoryByName");
        }

        return null;
    }

    public int createCategory(String categoryName, String description) {
        String cleanName = cleanString(categoryName);
        String cleanDescription = cleanString(description);

        if (!isValidCategoryName(cleanName)) {
            System.out.println("CategoryName khong hop le, toi da 100 ky tu.");
            return -1;
        }

        if (cleanDescription != null && cleanDescription.length() > 1000) {
            System.out.println("Description qua dai, toi da 500 ky tu.");
            return -1;
        }

        if (getCategoryByName(cleanName) != null) {
            System.out.println("CategoryName da ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO TOUR_CATEGORIES
                (
                    CategoryName,
                    Description
                )
                VALUES (?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, cleanName);
            ps.setString(2, cleanDescription);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            handleException(e, "createCategory");
        }

        return -1;
    }

    public boolean updateCategory(int categoryId, String categoryName, String description) {
        String cleanName = cleanString(categoryName);
        String cleanDescription = cleanString(description);

        if (categoryId <= 0) {
            System.out.println("CategoryID khong hop le.");
            return false;
        }

        if (!isValidCategoryName(cleanName)) {
            System.out.println("CategoryName khong hop le, toi da 100 ky tu.");
            return false;
        }

        if (cleanDescription != null && cleanDescription.length() > 1000) {
            System.out.println("Description qua dai, toi da 500 ky tu.");
            return false;
        }

        TourCategory current = getCategoryById(categoryId);

        if (current == null) {
            System.out.println("Khong tim thay category.");
            return false;
        }

        TourCategory duplicated = getCategoryByName(cleanName);

        if (duplicated != null && duplicated.getCategoryId() != categoryId) {
            System.out.println("CategoryName da ton tai o category khac.");
            return false;
        }

        String sql = """
                UPDATE TOUR_CATEGORIES
                SET CategoryName = ?,
                    Description = ?
                WHERE CategoryID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanName);
            ps.setString(2, cleanDescription);
            ps.setInt(3, categoryId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat category that bai.");
                return false;
            }

            return true;
        } catch (SQLException e) {
            handleException(e, "updateCategory");
        }

        return false;
    }

    public boolean deleteCategory(int categoryId) {
        if (categoryId <= 0) {
            System.out.println("CategoryID khong hop le.");
            return false;
        }

        if (isCategoryUsedByTour(categoryId)) {
            System.out.println("Khong the xoa category dang duoc tour su dung.");
            return false;
        }

        String sql = """
                DELETE FROM TOUR_CATEGORIES
                WHERE CategoryID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, categoryId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay category de xoa.");
                return false;
            }

            return true;
        } catch (SQLException e) {
            handleException(e, "deleteCategory");
        }

        return false;
    }

    /*
     * Giữ lại 2 hàm này để Main.java cũ không bị báo đỏ.
     * Vì bảng TOUR_CATEGORIES không có IsActive, hệ thống không thể bật/tắt category.
     */
    public boolean activateCategory(int categoryId) {
        if (categoryId <= 0) {
            System.out.println("CategoryID khong hop le.");
            return false;
        }

        System.out.println("TOUR_CATEGORIES khong co cot IsActive, khong can activate category.");
        return true;
    }

    public boolean deactivateCategory(int categoryId) {
        if (categoryId <= 0) {
            System.out.println("CategoryID khong hop le.");
            return false;
        }

        if (isCategoryUsedByTour(categoryId)) {
            System.out.println("Category dang duoc tour su dung. Bang TOUR_CATEGORIES khong co IsActive nen khong the an category.");
            return false;
        }

        System.out.println("TOUR_CATEGORIES khong co cot IsActive, khong can deactivate category.");
        return true;
    }

    public boolean isCategoryUsedByTour(int categoryId) {
        if (categoryId <= 0) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE CategoryID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total") > 0;
                }
            }
        } catch (SQLException e) {
            handleException(e, "isCategoryUsedByTour");
        }

        return false;
    }

    public int countToursByCategoryId(int categoryId) {
        if (categoryId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE CategoryID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total");
                }
            }
        } catch (SQLException e) {
            handleException(e, "countToursByCategoryId");
        }

        return 0;
    }

    /*
     * Giữ tên hàm cũ để các đoạn test trước đó không bị lỗi compile.
     * Nếu bảng TOURS có IsActive thì đếm tour active, nếu không có thì đếm toàn bộ tour.
     */
    public int countActiveToursByCategoryId(int categoryId) {
        if (categoryId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE CategoryID = ?
                  AND IsActive = 1
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total");
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 207) {
                return countToursByCategoryId(categoryId);
            }

            handleException(e, "countActiveToursByCategoryId");
        }

        return 0;
    }

    private List<TourCategory> queryCategoryList(String sql, SqlSetter setter, String methodName) {
        List<TourCategory> categories = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapTourCategory(rs));
                }
            }
        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return categories;
    }

    private TourCategory mapTourCategory(ResultSet rs) throws SQLException {
        TourCategory category = new TourCategory();

        category.setCategoryId(rs.getInt("CategoryID"));
        category.setCategoryName(rs.getString("CategoryName"));
        category.setDescription(rs.getString("Description"));

        return category;
    }

    private boolean isValidCategoryName(String categoryName) {
        return categoryName != null
                && !categoryName.trim().isEmpty()
                && categoryName.trim().length() <= 100;
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
            System.out.println("Loi " + methodName + ": Category dang duoc tham chieu hoac vi pham khoa ngoai.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": CategoryName da ton tai.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TOUR_CATEGORIES.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot trong database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TOUR_CATEGORIES.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }
}
