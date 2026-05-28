package dao;

import config.DatabaseConnection;
import model.AddOn;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class AddOnDAO {

    private static final int MAX_ADD_ON_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    public List<AddOn> getAllActiveAddOns() {
        String sql = buildSelectSql("""
                WHERE IsActive = 1
                ORDER BY AddOnID ASC
                """);

        return queryAddOnList(sql, null, "getAllActiveAddOns");
    }

    public List<AddOn> getAllAddOns() {
        String sql = buildSelectSql("""
                ORDER BY IsActive DESC, AddOnID ASC
                """);

        return queryAddOnList(sql, null, "getAllAddOns");
    }

    public AddOn getAddOnById(int addOnId) {
        if (addOnId <= 0) {
            System.out.println("AddOnID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE AddOnID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, addOnId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAddOn(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getAddOnById");
        }

        return null;
    }

    public AddOn getAddOnByName(String addOnName) {
        String cleanName = cleanString(addOnName);

        if (cleanName == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE AddOnName = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAddOn(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getAddOnByName");
        }

        return null;
    }

    public List<AddOn> searchAddOns(String keyword) {
        String cleanKeyword = cleanString(keyword);

        if (cleanKeyword == null) {
            return getAllActiveAddOns();
        }

        String sql = buildSelectSql("""
                WHERE IsActive = 1
                  AND (
                        AddOnName LIKE ?
                     OR Description LIKE ?
                  )
                ORDER BY AddOnID ASC
                """);

        return queryAddOnList(
                sql,
                ps -> {
                    String likeKeyword = "%" + cleanKeyword + "%";
                    ps.setString(1, likeKeyword);
                    ps.setString(2, likeKeyword);
                },
                "searchAddOns"
        );
    }

    public int createAddOn(String addOnName, BigDecimal price, String description) {
        AddOnInput input = validateAddOnInput(addOnName, price, description);

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        AddOn duplicated = getAddOnByName(input.addOnName);

        if (duplicated != null) {
            System.out.println("AddOnName da ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO ADD_ONS
                (
                    AddOnName,
                    Price,
                    Description,
                    IsActive
                )
                VALUES (?, ?, ?, 1)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, input.addOnName);
            ps.setBigDecimal(2, input.price);
            ps.setString(3, input.description);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createAddOn");
        }

        return -1;
    }

    public boolean updateAddOn(int addOnId,
                               String addOnName,
                               BigDecimal price,
                               String description) {
        if (addOnId <= 0) {
            System.out.println("AddOnID khong hop le.");
            return false;
        }

        AddOn current = getAddOnById(addOnId);

        if (current == null) {
            System.out.println("Khong tim thay add-on.");
            return false;
        }

        AddOnInput input = validateAddOnInput(addOnName, price, description);

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        AddOn duplicated = getAddOnByName(input.addOnName);

        if (duplicated != null && duplicated.getAddOnId() != addOnId) {
            System.out.println("AddOnName da ton tai o add-on khac.");
            return false;
        }

        String sql = """
                UPDATE ADD_ONS
                SET AddOnName = ?,
                    Price = ?,
                    Description = ?
                WHERE AddOnID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, input.addOnName);
            ps.setBigDecimal(2, input.price);
            ps.setString(3, input.description);
            ps.setInt(4, addOnId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat add-on that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateAddOn");
        }

        return false;
    }

    public boolean activateAddOn(int addOnId) {
        return updateActiveStatus(addOnId, true);
    }

    public boolean deactivateAddOn(int addOnId) {
        if (addOnId <= 0) {
            System.out.println("AddOnID khong hop le.");
            return false;
        }

        AddOn current = getAddOnById(addOnId);

        if (current == null) {
            System.out.println("Khong tim thay add-on.");
            return false;
        }

        return updateActiveStatus(addOnId, false);
    }

    public boolean deleteAddOn(int addOnId) {
        if (addOnId <= 0) {
            System.out.println("AddOnID khong hop le.");
            return false;
        }

        if (isAddOnUsedInBooking(addOnId)) {
            System.out.println("Add-on da duoc dung trong booking, khong the xoa. Hay deactivate.");
            return false;
        }

        String sql = """
                DELETE FROM ADD_ONS
                WHERE AddOnID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, addOnId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay add-on de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteAddOn");
        }

        return false;
    }

    public int countActiveAddOns() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM ADD_ONS
                WHERE IsActive = 1
                """;

        return queryCount(sql, null, "countActiveAddOns");
    }

    private boolean updateActiveStatus(int addOnId, boolean active) {
        if (addOnId <= 0) {
            System.out.println("AddOnID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE ADD_ONS
                SET IsActive = ?
                WHERE AddOnID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, addOnId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay add-on.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateActiveStatus");
        }

        return false;
    }

    private boolean isAddOnUsedInBooking(int addOnId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKING_ADD_ONS
                WHERE AddOnID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, addOnId),
                "isAddOnUsedInBooking"
        ) > 0;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    AddOnID,
                    AddOnName,
                    Price,
                    Description,
                    IsActive,
                    CreatedAt
                FROM ADD_ONS
                """ + condition;
    }

    private List<AddOn> queryAddOnList(String sql,
                                       SqlSetter setter,
                                       String methodName) {
        List<AddOn> addOns = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addOns.add(mapAddOn(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return addOns;
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

    private AddOn mapAddOn(ResultSet rs) throws SQLException {
        AddOn addOn = new AddOn();

        addOn.setAddOnId(rs.getInt("AddOnID"));
        addOn.setAddOnName(rs.getString("AddOnName"));
        addOn.setPrice(rs.getBigDecimal("Price"));
        addOn.setDescription(rs.getString("Description"));
        addOn.setActive(rs.getBoolean("IsActive"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            addOn.setCreatedAt(createdAt.toLocalDateTime());
        }

        return addOn;
    }

    private AddOnInput validateAddOnInput(String addOnName,
                                          BigDecimal price,
                                          String description) {
        String cleanName = cleanString(addOnName);
        String cleanDescription = cleanString(description);

        if (cleanName == null || cleanName.length() > MAX_ADD_ON_NAME_LENGTH) {
            return AddOnInput.invalid("AddOnName khong hop le, toi da 100 ky tu.");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return AddOnInput.invalid("Price khong hop le, phai >= 0.");
        }

        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            return AddOnInput.invalid("Description qua dai, toi da 1000 ky tu.");
        }

        return AddOnInput.valid(cleanName, price, cleanDescription);
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
            System.out.println("Loi " + methodName + ": AddOn dang duoc tham chieu hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": AddOnName hoac du lieu unique bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang ADD_ONS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang ADD_ONS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class AddOnInput {
        private final boolean valid;
        private final String message;
        private final String addOnName;
        private final BigDecimal price;
        private final String description;

        private AddOnInput(boolean valid,
                           String message,
                           String addOnName,
                           BigDecimal price,
                           String description) {
            this.valid = valid;
            this.message = message;
            this.addOnName = addOnName;
            this.price = price;
            this.description = description;
        }

        private static AddOnInput valid(String addOnName,
                                        BigDecimal price,
                                        String description) {
            return new AddOnInput(
                    true,
                    null,
                    addOnName,
                    price,
                    description
            );
        }

        private static AddOnInput invalid(String message) {
            return new AddOnInput(
                    false,
                    message,
                    null,
                    null,
                    null
            );
        }
    }
}
