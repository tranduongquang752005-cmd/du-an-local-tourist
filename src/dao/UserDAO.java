package dao;

import config.DatabaseConnection;
import model.User;
import util.AES256Util;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {


    private static final int MAX_FULL_NAME_LENGTH = 500;
    private static final int MAX_LOGIN_NAME_LENGTH = 100;
    private static final int MIN_INTERNAL_PASSWORD_LENGTH = 3;
    private static final int MIN_CUSTOMER_PASSWORD_LENGTH = 6;


    public User login(String identifier, String password) {
        String cleanIdentifier = cleanString(identifier);

        if (cleanIdentifier == null || isBlank(password)) {
            return null;
        }

        User user;

        if (isLoginName(cleanIdentifier)) {
            user = findUserByLoginName(cleanIdentifier);
        } else {
            user = findUserByPhone(cleanIdentifier);
        }

        if (user == null || !user.isActive()) {
            return null;
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            return null;
        }

        return user;
    }

    public int registerCustomer(String fullName, String phone, String password) {
        UserInput input = validateCustomerRegisterInput(fullName, phone, password);

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (phoneExists(input.phone)) {
            System.out.println("So dien thoai da ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO USERS
                (
                    FullName,
                    Phone,
                    LoginName,
                    PasswordHash,
                    Role,
                    IsActive,
                    MaxPendingBookings
                )
                VALUES (?, ?, NULL, ?, 'CUSTOMER', 1, 3)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, encryptFullName(input.fullName));
            ps.setString(2, input.phone);
            ps.setString(3, PasswordUtil.hashPassword(input.password));

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "registerCustomer");
        }

        return -1;
    }

    public boolean phoneExists(String phone) {
        String cleanPhone = cleanString(phone);

        if (cleanPhone == null) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE Phone = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setString(1, cleanPhone),
                "phoneExists"
        ) > 0;
    }

    public boolean loginNameExists(String loginName) {
        String cleanLoginName = normalizeLoginName(loginName);

        if (cleanLoginName == null) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE LoginName = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setString(1, cleanLoginName),
                "loginNameExists"
        ) > 0;
    }

    public User findUserByPhone(String phone) {
        String cleanPhone = cleanString(phone);

        if (cleanPhone == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE Phone = ?
                """);

        return querySingleUser(
                sql,
                ps -> ps.setString(1, cleanPhone),
                "findUserByPhone"
        );
    }

    public User findUserByLoginName(String loginName) {
        String cleanLoginName = normalizeLoginName(loginName);

        if (cleanLoginName == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE LoginName = ?
                """);

        return querySingleUser(
                sql,
                ps -> ps.setString(1, cleanLoginName),
                "findUserByLoginName"
        );
    }

    public User getUserById(int userId) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE UserID = ?
                """);

        return querySingleUser(
                sql,
                ps -> ps.setInt(1, userId),
                "getUserById"
        );
    }

    public List<User> getAllUsers() {
        String sql = buildSelectSql("""
                ORDER BY UserID ASC
                """);

        return queryUserList(sql, null, "getAllUsers");
    }

    public List<User> getAllCustomers() {
        String sql = buildSelectSql("""
                WHERE Role = 'CUSTOMER'
                ORDER BY UserID ASC
                """);

        return queryUserList(sql, null, "getAllCustomers");
    }

    public List<User> getAllStaff() {
        String sql = buildSelectSql("""
                WHERE Role = 'STAFF'
                ORDER BY UserID ASC
                """);

        return queryUserList(sql, null, "getAllStaff");
    }

    public List<User> getAllManagers() {
        String sql = buildSelectSql("""
                WHERE Role = 'MANAGER'
                ORDER BY UserID ASC
                """);

        return queryUserList(sql, null, "getAllManagers");
    }

    public boolean updateCustomerInfo(int userId, String fullName, String phone) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return false;
        }

        User current = getUserById(userId);

        if (current == null) {
            System.out.println("Khong tim thay user.");
            return false;
        }

        if (!current.isCustomer()) {
            System.out.println("Chi cap nhat thong tin customer bang ham nay.");
            return false;
        }

        String cleanFullName = cleanString(fullName);
        String cleanPhone = cleanString(phone);

        if (!isValidFullName(cleanFullName)) {
            System.out.println("Ho ten khong hop le.");
            return false;
        }

        if (!isValidPhone(cleanPhone)) {
            System.out.println("So dien thoai khong hop le.");
            return false;
        }

        User duplicated = findUserByPhone(cleanPhone);

        if (duplicated != null && duplicated.getUserId() != userId) {
            System.out.println("So dien thoai da thuoc user khac.");
            return false;
        }

        String sql = """
                UPDATE USERS
                SET FullName = ?,
                    Phone = ?
                WHERE UserID = ?
                  AND Role = 'CUSTOMER'
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, encryptFullName(cleanFullName));
            ps.setString(2, cleanPhone);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "updateCustomerInfo");
        }

        return false;
    }

    public boolean updatePassword(int userId, String newPassword) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return false;
        }

        if (isBlank(newPassword) || newPassword.length() < MIN_INTERNAL_PASSWORD_LENGTH) {
            System.out.println("Mat khau khong hop le.");
            return false;
        }

        String sql = """
                UPDATE USERS
                SET PasswordHash = ?
                WHERE UserID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, PasswordUtil.hashPassword(newPassword));
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "updatePassword");
        }

        return false;
    }

    public boolean updatePasswordByPhone(String phone, String newPassword) {
        User user = findUserByPhone(phone);

        if (user == null) {
            System.out.println("Khong tim thay user theo phone.");
            return false;
        }

        return updatePassword(user.getUserId(), newPassword);
    }

    public boolean updatePasswordByLoginName(String loginName, String newPassword) {
        User user = findUserByLoginName(loginName);

        if (user == null) {
            System.out.println("Khong tim thay user theo LoginName.");
            return false;
        }

        return updatePassword(user.getUserId(), newPassword);
    }

    public boolean activateUser(int userId) {
        return updateActiveStatus(userId, true);
    }

    public boolean deactivateUser(int userId) {
        return updateActiveStatus(userId, false);
    }

    public int countCustomers() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE Role = 'CUSTOMER'
                """;

        return queryCount(sql, null, "countCustomers");
    }

    public int countActiveUsers() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE IsActive = 1
                """;

        return queryCount(sql, null, "countActiveUsers");
    }

    private boolean updateActiveStatus(int userId, boolean active) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE USERS
                SET IsActive = ?
                WHERE UserID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "updateActiveStatus");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    UserID,
                    FullName,
                    Phone,
                    LoginName,
                    PasswordHash,
                    Role,
                    IsActive,
                    MaxPendingBookings,
                    CreatedAt
                FROM USERS
                """ + condition;
    }

    private User querySingleUser(String sql, SqlSetter setter, String methodName) {
        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return null;
    }

    private List<User> queryUserList(String sql, SqlSetter setter, String methodName) {
        List<User> users = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return users;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setUserId(rs.getInt("UserID"));
        user.setFullName(decryptFullName(rs.getString("FullName")));
        user.setPhone(rs.getString("Phone"));
        user.setLoginName(rs.getString("LoginName"));
        user.setPasswordHash(rs.getString("PasswordHash"));
        user.setRole(rs.getString("Role"));
        user.setActive(rs.getBoolean("IsActive"));
        user.setMaxPendingBookings(rs.getInt("MaxPendingBookings"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        return user;
    }

    private UserInput validateCustomerRegisterInput(String fullName, String phone, String password) {
        String cleanFullName = cleanString(fullName);
        String cleanPhone = cleanString(phone);

        if (!isValidFullName(cleanFullName)) {
            return UserInput.invalid("Ho ten khong hop le.");
        }

        if (!isValidPhone(cleanPhone)) {
            return UserInput.invalid("So dien thoai khong hop le.");
        }

        if (isBlank(password) || password.length() < MIN_CUSTOMER_PASSWORD_LENGTH) {
            return UserInput.invalid("Mat khau customer phai co it nhat 6 ky tu.");
        }

        return UserInput.valid(cleanFullName, cleanPhone, password);
    }

    private int queryCount(String sql, SqlSetter setter, String methodName) {
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

    private boolean isLoginName(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    private String normalizeLoginName(String loginName) {
        String cleanLoginName = cleanString(loginName);

        if (cleanLoginName == null || cleanLoginName.length() > MAX_LOGIN_NAME_LENGTH) {
            return null;
        }

        return cleanLoginName.toLowerCase();
    }

    private boolean isValidFullName(String fullName) {
        return fullName != null && fullName.length() <= MAX_FULL_NAME_LENGTH;
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^0\\d{9}$");
    }

    private String encryptFullName(String fullName) {
        try {
            return AES256Util.encrypt(fullName);
        } catch (Exception e) {
            return fullName;
        }
    }

    private String decryptFullName(String fullName) {
        try {
            return AES256Util.decrypt(fullName);
        } catch (Exception e) {
            return fullName;
        }
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Phone hoac LoginName bi trung.");
        } else if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": Du lieu user vi pham CHECK/FOREIGN KEY constraint.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot SQL. Kiem tra bang USERS co cot LoginName chua.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu user qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang USERS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class UserInput {
        private final boolean valid;
        private final String message;
        private final String fullName;
        private final String phone;
        private final String password;

        private UserInput(boolean valid, String message, String fullName, String phone, String password) {
            this.valid = valid;
            this.message = message;
            this.fullName = fullName;
            this.phone = phone;
            this.password = password;
        }

        private static UserInput valid(String fullName, String phone, String password) {
            return new UserInput(true, null, fullName, phone, password);
        }

        private static UserInput invalid(String message) {
            return new UserInput(false, message, null, null, null);
        }
    }
}
