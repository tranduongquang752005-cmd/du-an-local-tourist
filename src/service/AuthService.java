package service;

import dao.UserDAO;
import model.User;
import util.PasswordUtil;

import java.util.regex.Pattern;

public class AuthService {

    public static final String STAFF_LOGIN_IDENTIFIER = "staff@gmail.com";
    public static final String MANAGER_LOGIN_IDENTIFIER = "admin@gmail.com";

    private static final int MIN_CUSTOMER_PASSWORD_LENGTH = 6;
    private static final Pattern CUSTOMER_PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    private final UserDAO userDAO;
    private final PermissionService permissionService;

    public AuthService() {
        this(new UserDAO(), new PermissionService());
    }

    public AuthService(UserDAO userDAO, PermissionService permissionService) {
        this.userDAO = userDAO == null ? new UserDAO() : userDAO;
        this.permissionService = permissionService == null ? new PermissionService() : permissionService;
    }

    public AuthResult registerCustomer(String fullName, String phone, String password) {
        String cleanFullName = cleanString(fullName);
        String cleanPhone = cleanString(phone);

        if (cleanFullName == null) {
            return AuthResult.fail("REGISTER_FULLNAME_EMPTY", "Ho ten khong duoc de trong.");
        }

        if (!isValidCustomerPhone(cleanPhone)) {
            return AuthResult.fail("REGISTER_PHONE_INVALID", "So dien thoai khong hop le. Dinh dang dung: 0xxxxxxxxx.");
        }

        if (!isValidCustomerPassword(password)) {
            return AuthResult.fail(
                    "REGISTER_PASSWORD_WEAK",
                    "Mat khau khach hang phai co it nhat " + MIN_CUSTOMER_PASSWORD_LENGTH + " ky tu."
            );
        }

        User existedUser = userDAO.findUserByPhone(cleanPhone);

        if (existedUser != null) {
            return AuthResult.fail("REGISTER_PHONE_EXISTS", "So dien thoai da duoc dang ky.");
        }

        int userId = userDAO.registerCustomer(cleanFullName, cleanPhone, password);

        if (userId <= 0) {
            return AuthResult.fail("REGISTER_FAILED", "Dang ky khach hang that bai.");
        }

        User createdUser = userDAO.getUserById(userId);

        if (createdUser == null) {
            return AuthResult.success(null, "REGISTER_SUCCESS_RELOGIN", "Dang ky thanh cong. Vui long dang nhap lai.");
        }

        if (!createdUser.isCustomer()) {
            return AuthResult.fail("REGISTER_ROLE_INVALID", "Tai khoan vua tao khong phai CUSTOMER. Kiem tra UserDAO hoac SQL.");
        }

        if (!createdUser.isActive()) {
            return AuthResult.fail("REGISTER_USER_INACTIVE", "Tai khoan vua tao dang bi khoa.");
        }

        return AuthResult.success(createdUser, "REGISTER_SUCCESS", "Dang ky khach hang thanh cong.");
    }

    public AuthResult login(String identifier, String password) {
        String cleanIdentifier = normalizeIdentifier(identifier);

        if (cleanIdentifier == null) {
            return AuthResult.fail("LOGIN_IDENTIFIER_EMPTY", "Tai khoan dang nhap khong duoc de trong.");
        }

        if (isStaffLoginIdentifier(cleanIdentifier)) {
            return loginStaff(cleanIdentifier, password);
        }

        if (isManagerLoginIdentifier(cleanIdentifier)) {
            return loginManager(cleanIdentifier, password);
        }

        return loginCustomer(cleanIdentifier, password);
    }

    public AuthResult loginCustomer(String phone, String password) {
        String cleanPhone = cleanString(phone);

        if (!isValidCustomerPhone(cleanPhone)) {
            return AuthResult.fail("CUSTOMER_PHONE_INVALID", "Khach hang phai dang nhap bang so dien thoai hop le.");
        }

        if (isBlank(password)) {
            return AuthResult.fail("PASSWORD_EMPTY", "Mat khau khong duoc de trong.");
        }

        User user = userDAO.findUserByPhone(cleanPhone);

        if (user == null) {
            return AuthResult.fail("CUSTOMER_NOT_FOUND", "So dien thoai hoac mat khau khong dung.");
        }

        if (!user.isCustomer()) {
            return AuthResult.fail("CUSTOMER_ROLE_INVALID", "Tai khoan nay khong phai CUSTOMER.");
        }

        return authenticateUser(user, password, "Dang nhap khach hang thanh cong.");
    }

    public AuthResult loginStaff(String loginName, String password) {
        String cleanLoginName = normalizeIdentifier(loginName);

        if (!isStaffLoginIdentifier(cleanLoginName)) {
            return AuthResult.fail("STAFF_LOGIN_INVALID", "Tai khoan nhan vien khong hop le.");
        }

        return loginInternal(cleanLoginName, password, User.Role.STAFF);
    }

    public AuthResult loginManager(String loginName, String password) {
        String cleanLoginName = normalizeIdentifier(loginName);

        if (!isManagerLoginIdentifier(cleanLoginName)) {
            return AuthResult.fail("MANAGER_LOGIN_INVALID", "Tai khoan truong phong khong hop le.");
        }

        return loginInternal(cleanLoginName, password, User.Role.MANAGER);
    }

    private AuthResult loginInternal(String loginName, String password, User.Role requiredRole) {
        String cleanLoginName = normalizeIdentifier(loginName);

        if (!isAllowedInternalLoginName(cleanLoginName)) {
            return AuthResult.fail("INTERNAL_LOGIN_INVALID", "Tai khoan noi bo khong hop le.");
        }

        if (isBlank(password)) {
            return AuthResult.fail("PASSWORD_EMPTY", "Mat khau khong duoc de trong.");
        }

        User user = userDAO.findUserByLoginName(cleanLoginName);

        if (user == null) {
            return AuthResult.fail("INTERNAL_NOT_FOUND", "Tai khoan noi bo hoac mat khau khong dung.");
        }

        if (!user.hasRole(requiredRole)) {
            return AuthResult.fail("INTERNAL_ROLE_INVALID", "Tai khoan noi bo khong dung role " + requiredRole.name() + ".");
        }

        return authenticateUser(user, password, "Dang nhap noi bo thanh cong.");
    }

    private AuthResult authenticateUser(User user, String rawPassword, String successMessage) {
        if (user == null) {
            return AuthResult.fail("AUTH_USER_NULL", "Khong tim thay tai khoan.");
        }

        if (!permissionService.isActive(user)) {
            return AuthResult.fail("AUTH_USER_INACTIVE", "Tai khoan da bi khoa.");
        }

        if (isBlank(user.getPasswordHash())) {
            return AuthResult.fail("AUTH_PASSWORD_HASH_EMPTY", "Tai khoan chua co PasswordHash hop le trong database.");
        }

        PasswordCheck passwordCheck = verifyPasswordSafely(rawPassword, user.getPasswordHash());

        if (!passwordCheck.validFormat) {
            return AuthResult.fail(
                    "AUTH_PASSWORD_HASH_INVALID",
                    "PasswordHash trong database khong dung dinh dang. Hay tao lai hash bang PasswordUtil.hashPassword()."
            );
        }

        if (!passwordCheck.matched) {
            return AuthResult.fail("AUTH_PASSWORD_WRONG", "Tai khoan hoac mat khau khong dung.");
        }

        return AuthResult.success(user, "AUTH_SUCCESS", successMessage);
    }

    public AuthDebugResult debugLogin(String identifier, String password) {
        String cleanIdentifier = normalizeIdentifier(identifier);

        if (cleanIdentifier == null) {
            return AuthDebugResult.of(false, "Identifier rong.", null);
        }

        User user;

        if (isStaffLoginIdentifier(cleanIdentifier) || isManagerLoginIdentifier(cleanIdentifier)) {
            user = userDAO.findUserByLoginName(cleanIdentifier);
        } else if (isValidCustomerPhone(cleanIdentifier)) {
            user = userDAO.findUserByPhone(cleanIdentifier);
        } else {
            return AuthDebugResult.of(false, "Identifier khong phai phone hop le hoac internal login name.", null);
        }

        if (user == null) {
            return AuthDebugResult.of(false, "Khong tim thay user trong database.", null);
        }

        if (!user.isActive()) {
            return AuthDebugResult.of(false, "User ton tai nhung IsActive = false.", user);
        }

        if (isBlank(user.getPasswordHash())) {
            return AuthDebugResult.of(false, "PasswordHash rong/null.", user);
        }

        PasswordCheck check = verifyPasswordSafely(password, user.getPasswordHash());

        if (!check.validFormat) {
            return AuthDebugResult.of(false, "PasswordHash sai format, khong verify duoc.", user);
        }

        if (!check.matched) {
            return AuthDebugResult.of(false, "Password khong khop PasswordHash.", user);
        }

        return AuthDebugResult.of(true, "Login debug OK.", user);
    }

    private PasswordCheck verifyPasswordSafely(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) {
            return PasswordCheck.invalidFormat();
        }

        try {
            boolean matched = PasswordUtil.verifyPassword(rawPassword, passwordHash);
            return PasswordCheck.valid(matched);
        } catch (Exception e) {
            return PasswordCheck.invalidFormat();
        }
    }

    public AuthResult loginDefaultStaff(String password) {
        return loginStaff(STAFF_LOGIN_IDENTIFIER, password);
    }

    public AuthResult loginDefaultManager(String password) {
        return loginManager(MANAGER_LOGIN_IDENTIFIER, password);
    }

    public boolean isCustomerLoginIdentifier(String identifier) {
        return isValidCustomerPhone(cleanString(identifier));
    }

    public boolean isStaffLoginIdentifier(String identifier) {
        return STAFF_LOGIN_IDENTIFIER.equals(normalizeIdentifier(identifier));
    }

    public boolean isManagerLoginIdentifier(String identifier) {
        return MANAGER_LOGIN_IDENTIFIER.equals(normalizeIdentifier(identifier));
    }

    public String getStaffLoginIdentifier() {
        return STAFF_LOGIN_IDENTIFIER;
    }

    public String getManagerLoginIdentifier() {
        return MANAGER_LOGIN_IDENTIFIER;
    }

    private boolean isAllowedInternalLoginName(String loginName) {
        return isStaffLoginIdentifier(loginName) || isManagerLoginIdentifier(loginName);
    }

    private String normalizeIdentifier(String identifier) {
        String cleanIdentifier = cleanString(identifier);

        if (cleanIdentifier == null) {
            return null;
        }

        return cleanIdentifier.toLowerCase();
    }

    private boolean isValidCustomerPhone(String phone) {
        return phone != null && CUSTOMER_PHONE_PATTERN.matcher(phone).matches();
    }

    private boolean isValidCustomerPassword(String password) {
        return password != null && password.length() >= MIN_CUSTOMER_PASSWORD_LENGTH;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class PasswordCheck {
        private final boolean validFormat;
        private final boolean matched;

        private PasswordCheck(boolean validFormat, boolean matched) {
            this.validFormat = validFormat;
            this.matched = matched;
        }

        private static PasswordCheck valid(boolean matched) {
            return new PasswordCheck(true, matched);
        }

        private static PasswordCheck invalidFormat() {
            return new PasswordCheck(false, false);
        }
    }

    public static class AuthResult {
        private final boolean success;
        private final String code;
        private final String message;
        private final User user;

        private AuthResult(boolean success, String code, String message, User user) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.user = user;
        }

        public static AuthResult success(User user, String code, String message) {
            return new AuthResult(true, code, message, user);
        }

        public static AuthResult success(User user, String message) {
            return new AuthResult(true, "SUCCESS", message, user);
        }

        public static AuthResult fail(String code, String message) {
            return new AuthResult(false, code, message, null);
        }

        public static AuthResult fail(String message) {
            return new AuthResult(false, "FAILED", message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }

        @Override
        public String toString() {
            return "AuthResult{" +
                    "success=" + success +
                    ", code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    ", user=" + user +
                    '}';
        }
    }

    public static class AuthDebugResult {
        private final boolean success;
        private final String message;
        private final User user;

        private AuthDebugResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static AuthDebugResult of(boolean success, String message, User user) {
            return new AuthDebugResult(success, message, user);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }

        @Override
        public String toString() {
            return "AuthDebugResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", user=" + user +
                    '}';
        }
    }
}
