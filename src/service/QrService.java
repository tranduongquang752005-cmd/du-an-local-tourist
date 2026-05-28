package service;

import dao.BookingDAO;
import dao.ETicketDAO;
import model.Booking;
import model.ETicket;
import model.User;
import util.QRCodeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QrService {

    private static final int DEFAULT_QR_SIZE = 320;
    private static final int MAX_BANK_CODE_LENGTH = 30;
    private static final int MAX_ACCOUNT_NUMBER_LENGTH = 30;
    private static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    private static final int MAX_TRANSFER_CONTENT_LENGTH = 100;

    private static final String PAYMENT_QR_TYPE = "PAYMENT";
    private static final String TICKET_QR_TYPE = "TICKET";

    /*
     * Dung cho mo phong/checksum noi bo.
     * Neu sau nay len production, nen dua gia tri nay vao SystemConfig hoac bien moi truong.
     */
    private static final String QR_INTERNAL_SECRET = "DU_LICH_QR_INTERNAL_SECRET_V1";

    private final PermissionService permissionService;
    private final BookingDAO bookingDAO;
    private final ETicketDAO eTicketDAO;

    public QrService() {
        this.permissionService = new PermissionService();
        this.bookingDAO = new BookingDAO();
        this.eTicketDAO = new ETicketDAO();
    }

    /*
     * QR THANH TOAN MO PHONG:
     * - Khong goi ngan hang that.
     * - Khong lien ket API ngan hang.
     * - Amount luon lay tu Booking.FinalPrice.
     * - Chi tao duoc khi booking dang PENDING.
     * - FE hien thi qrImageDataUri bang img src.
     */
    public ServiceResult<QrInfo> createPaymentQr(User currentUser,
                                                 int bookingId,
                                                 String bankCode,
                                                 String accountNumber,
                                                 String accountName) {
        if (!permissionService.canViewBookingDetailForOperation(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc tao QR thanh toan.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        Booking booking;

        try {
            booking = bookingDAO.getBookingById(bookingId);
        } catch (Exception e) {
            return ServiceResult.fail("BOOKING_GET_FAILED", "Lay booking that bai: " + safeExceptionMessage(e));
        }

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi tao QR thanh toan cho booking PENDING.");
        }

        BigDecimal payableAmount = normalizeMoney(booking.getFinalPrice());

        if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ServiceResult.fail("AMOUNT_INVALID", "FinalPrice cua booking khong hop le.");
        }

        BankInputValidation validation = validateBankInput(bankCode, accountNumber, accountName);

        if (!validation.valid) {
            return ServiceResult.fail(validation.code, validation.message);
        }

        String transferContent = buildPaymentContent(bookingId);
        String rawPayload = buildPaymentPayload(
                bookingId,
                validation.bankCode,
                validation.accountNumber,
                validation.accountName,
                payableAmount,
                transferContent
        );

        String payload = rawPayload + "|CHECKSUM=" + checksum(rawPayload);

        return buildQrInfo(
                PAYMENT_QR_TYPE,
                bookingId,
                null,
                payableAmount,
                transferContent,
                payload,
                "Tao QR thanh toan mo phong thanh cong."
        );
    }

    /*
     * QR VE CHO CUSTOMER:
     * - Customer chi lay duoc QR ve cua chinh minh.
     * - Ticket phai ton tai sau khi booking da thanh toan thanh cong.
     * - Ticket phai ACTIVE.
     */
    public ServiceResult<QrInfo> createMyTicketQr(User currentUser, int bookingId) {
        if (!permissionService.canViewOwnBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi khach hang moi duoc xem QR ve cua minh.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        Booking booking;

        try {
            booking = bookingDAO.getBookingById(bookingId);
        } catch (Exception e) {
            return ServiceResult.fail("BOOKING_GET_FAILED", "Lay booking that bai: " + safeExceptionMessage(e));
        }

        if (booking == null || booking.getUserId() != currentUser.getUserId()) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        return createTicketQrByBookingIdInternal(bookingId);
    }

    /*
     * QR VE CHO STAFF/MANAGER:
     * - Dung khi nhan vien can in ve, kiem tra ve hoac ho tro khach.
     */
    public ServiceResult<QrInfo> createTicketQrForOperation(User currentUser, int bookingId) {
        if (!permissionService.canViewOperationalBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xem QR ve.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        return createTicketQrByBookingIdInternal(bookingId);
    }

    private ServiceResult<QrInfo> createTicketQrByBookingIdInternal(int bookingId) {
        ETicket ticket;

        try {
            ticket = eTicketDAO.getTicketByBookingId(bookingId);
        } catch (Exception e) {
            return ServiceResult.fail("TICKET_GET_FAILED", "Lay ve dien tu that bai: " + safeExceptionMessage(e));
        }

        if (ticket == null) {
            return ServiceResult.fail("TICKET_NOT_FOUND", "Booking chua co ve dien tu.");
        }

        String ticketCode = cleanString(ticket.getTicketCode());
        String qrCode = cleanString(ticket.getQrCode());

        if (ticketCode == null) {
            return ServiceResult.fail("TICKET_CODE_EMPTY", "TicketCode khong hop le.");
        }

        if (!isActiveTicket(ticket)) {
            return ServiceResult.fail("TICKET_NOT_ACTIVE", "Ve dien tu khong con ACTIVE.");
        }

        String rawPayload = buildTicketPayload(ticket);
        String payload = rawPayload + "|CHECKSUM=" + checksum(rawPayload);

        return buildQrInfo(
                TICKET_QR_TYPE,
                bookingId,
                ticketCode,
                null,
                qrCode,
                payload,
                "Tao QR ve dien tu thanh cong."
        );
    }

    private ServiceResult<QrInfo> buildQrInfo(String qrType,
                                              int bookingId,
                                              String ticketCode,
                                              BigDecimal amount,
                                              String displayContent,
                                              String payload,
                                              String successMessage) {
        try {
            String qrImageBase64 = QRCodeUtil.toPngBase64(payload, DEFAULT_QR_SIZE, DEFAULT_QR_SIZE);
            String qrImageDataUri = "data:image/png;base64," + qrImageBase64;

            QrInfo qrInfo = new QrInfo(
                    qrType,
                    bookingId,
                    ticketCode,
                    amount,
                    displayContent,
                    payload,
                    qrImageBase64,
                    qrImageDataUri
            );

            return ServiceResult.success(qrInfo, successMessage);

        } catch (Exception e) {
            return ServiceResult.fail("QR_CREATE_FAILED", "Tao QR that bai: " + safeExceptionMessage(e));
        }
    }

    private String buildPaymentPayload(int bookingId,
                                       String bankCode,
                                       String accountNumber,
                                       String accountName,
                                       BigDecimal amount,
                                       String transferContent) {
        return "QR_TYPE=PAYMENT_SIMULATION"
                + "|BOOKING_ID=" + bookingId
                + "|BANK_CODE=" + bankCode
                + "|ACCOUNT_NUMBER=" + accountNumber
                + "|ACCOUNT_NAME=" + accountName
                + "|AMOUNT=" + amount.setScale(0, RoundingMode.HALF_UP)
                + "|CONTENT=" + transferContent;
    }

    private String buildTicketPayload(ETicket ticket) {
        StringBuilder builder = new StringBuilder();

        builder.append("QR_TYPE=TICKET");
        builder.append("|BOOKING_ID=").append(ticket.getBookingId());
        builder.append("|TICKET_CODE=").append(cleanString(ticket.getTicketCode()));
        builder.append("|QR_CODE=").append(cleanString(ticket.getQrCode()));
        builder.append("|STATUS=").append(ticket.getTicketStatus());

        if (ticket.getIssuedDate() != null) {
            builder.append("|ISSUED_DATE=").append(ticket.getIssuedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (ticket.getExpiryDate() != null) {
            builder.append("|EXPIRY_DATE=").append(ticket.getExpiryDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        return builder.toString();
    }

    private String buildPaymentContent(int bookingId) {
        String content = "BOOKING_" + bookingId;

        if (content.length() > MAX_TRANSFER_CONTENT_LENGTH) {
            return content.substring(0, MAX_TRANSFER_CONTENT_LENGTH);
        }

        return content;
    }

    private boolean isActiveTicket(ETicket ticket) {
        if (ticket == null || ticket.getTicketStatus() == null) {
            return false;
        }

        if (!"ACTIVE".equalsIgnoreCase(String.valueOf(ticket.getTicketStatus()))) {
            return false;
        }

        LocalDateTime expiryDate = ticket.getExpiryDate();

        return expiryDate == null || expiryDate.isAfter(LocalDateTime.now());
    }

    private BankInputValidation validateBankInput(String bankCode, String accountNumber, String accountName) {
        String cleanBankCode = cleanString(bankCode);
        String cleanAccountNumber = cleanString(accountNumber);
        String cleanAccountName = cleanString(accountName);

        if (cleanBankCode == null) {
            return BankInputValidation.invalid("BANK_CODE_EMPTY", "Ma ngan hang khong duoc de trong.");
        }

        if (cleanAccountNumber == null) {
            return BankInputValidation.invalid("ACCOUNT_NUMBER_EMPTY", "So tai khoan khong duoc de trong.");
        }

        if (cleanAccountName == null) {
            return BankInputValidation.invalid("ACCOUNT_NAME_EMPTY", "Ten chu tai khoan khong duoc de trong.");
        }

        if (cleanBankCode.length() > MAX_BANK_CODE_LENGTH) {
            return BankInputValidation.invalid("BANK_CODE_TOO_LONG", "Ma ngan hang qua dai.");
        }

        if (cleanAccountNumber.length() > MAX_ACCOUNT_NUMBER_LENGTH) {
            return BankInputValidation.invalid("ACCOUNT_NUMBER_TOO_LONG", "So tai khoan qua dai.");
        }

        if (cleanAccountName.length() > MAX_ACCOUNT_NAME_LENGTH) {
            return BankInputValidation.invalid("ACCOUNT_NAME_TOO_LONG", "Ten chu tai khoan qua dai.");
        }

        if (!cleanBankCode.matches("^[A-Za-z0-9_\\-]+$")) {
            return BankInputValidation.invalid("BANK_CODE_INVALID", "Ma ngan hang chi nen gom chu, so, gach ngang/gach duoi.");
        }

        if (!cleanAccountNumber.matches("^[0-9]{4,30}$")) {
            return BankInputValidation.invalid("ACCOUNT_NUMBER_INVALID", "So tai khoan chi duoc gom 4 den 30 chu so.");
        }

        return BankInputValidation.valid(cleanBankCode, cleanAccountNumber, cleanAccountName);
    }

    private boolean isPendingBooking(Booking booking) {
        return booking != null
                && booking.getStatus() != null
                && "PENDING".equalsIgnoreCase(String.valueOf(booking.getStatus()));
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String checksum(String value) {
        String source = value + "|" + QR_INTERNAL_SECRET;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }

            return hex.substring(0, 16);

        } catch (Exception e) {
            return "CHECKSUM_ERROR";
        }
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeExceptionMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "Loi khong xac dinh.";
        }

        String message = e.getMessage().replaceAll("[\\r\\n\\t]+", " ").trim();

        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }

        return message;
    }

    private static class BankInputValidation {
        private final boolean valid;
        private final String code;
        private final String message;
        private final String bankCode;
        private final String accountNumber;
        private final String accountName;

        private BankInputValidation(boolean valid,
                                    String code,
                                    String message,
                                    String bankCode,
                                    String accountNumber,
                                    String accountName) {
            this.valid = valid;
            this.code = code;
            this.message = message;
            this.bankCode = bankCode;
            this.accountNumber = accountNumber;
            this.accountName = accountName;
        }

        private static BankInputValidation valid(String bankCode, String accountNumber, String accountName) {
            return new BankInputValidation(true, null, null, bankCode, accountNumber, accountName);
        }

        private static BankInputValidation invalid(String code, String message) {
            return new BankInputValidation(false, code, message, null, null, null);
        }
    }

    public static class QrInfo {
        private final String qrType;
        private final int bookingId;
        private final String ticketCode;
        private final BigDecimal amount;
        private final String displayContent;
        private final String qrPayload;
        private final String qrImageBase64;
        private final String qrImageDataUri;

        public QrInfo(String qrType,
                      int bookingId,
                      String ticketCode,
                      BigDecimal amount,
                      String displayContent,
                      String qrPayload,
                      String qrImageBase64,
                      String qrImageDataUri) {
            this.qrType = qrType;
            this.bookingId = bookingId;
            this.ticketCode = ticketCode;
            this.amount = amount;
            this.displayContent = displayContent;
            this.qrPayload = qrPayload;
            this.qrImageBase64 = qrImageBase64;
            this.qrImageDataUri = qrImageDataUri;
        }

        public String getQrType() {
            return qrType;
        }

        public int getBookingId() {
            return bookingId;
        }

        public String getTicketCode() {
            return ticketCode;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getDisplayContent() {
            return displayContent;
        }

        public String getQrPayload() {
            return qrPayload;
        }

        public String getQrImageBase64() {
            return qrImageBase64;
        }

        public String getQrImageDataUri() {
            return qrImageDataUri;
        }

        @Override
        public String toString() {
            return "QrInfo{" +
                    "qrType='" + qrType + '\'' +
                    ", bookingId=" + bookingId +
                    ", ticketCode='" + ticketCode + '\'' +
                    ", amount=" + amount +
                    ", displayContent='" + displayContent + '\'' +
                    ", qrPayload='" + qrPayload + '\'' +
                    ", qrImageBase64Length=" + (qrImageBase64 == null ? 0 : qrImageBase64.length()) +
                    ", qrImageDataUriLength=" + (qrImageDataUri == null ? 0 : qrImageDataUri.length()) +
                    '}';
        }
    }

    public static class ServiceResult<T> {
        private final boolean success;
        private final String code;
        private final String message;
        private final T data;

        private ServiceResult(boolean success, String code, String message, T data) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public static <T> ServiceResult<T> success(T data, String message) {
            return new ServiceResult<>(true, "SUCCESS", message, data);
        }

        public static <T> ServiceResult<T> fail(String code, String message) {
            return new ServiceResult<>(false, code, message, null);
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

        public T getData() {
            return data;
        }

        @Override
        public String toString() {
            return "ServiceResult{" +
                    "success=" + success +
                    ", code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}
