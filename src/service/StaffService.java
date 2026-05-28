package service;

import dao.BookingCancellationDAO;
import dao.BookingDAO;
import dao.PaymentDAO;
import dao.RefundDAO;
import dao.TransportRequestDAO;

import model.Booking;
import model.Payment;
import model.Refund;
import model.TransportRequest;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StaffService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String CANCEL_BY_COMPANY = "COMPANY";

    private static final String DEFAULT_PAYMENT_METHOD_BANKING = "BANKING";
    private static final String DEFAULT_PAYMENT_METHOD_CASH = "CASH";

    private static final int MAX_BANK_CODE_LENGTH = 30;
    private static final int MAX_ACCOUNT_NUMBER_LENGTH = 30;
    private static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    private static final int MAX_TRANSFER_CONTENT_LENGTH = 100;

    private final PermissionService permissionService;
    private final BookingDAO bookingDAO;
    private final PaymentDAO paymentDAO;
    private final TransportRequestDAO transportRequestDAO;
    private final BookingCancellationDAO bookingCancellationDAO;
    private final RefundDAO refundDAO;

    public StaffService() {
        this.permissionService = new PermissionService();
        this.bookingDAO = new BookingDAO();
        this.paymentDAO = new PaymentDAO();
        this.transportRequestDAO = new TransportRequestDAO();
        this.bookingCancellationDAO = new BookingCancellationDAO();
        this.refundDAO = new RefundDAO();
    }

    /*
     * STAFF/MANAGER xem chi tiết booking để hỗ trợ vận hành.
     */
    public ServiceResult<Booking> getBookingDetail(User currentUser, int bookingId) {
        if (!permissionService.canViewBookingDetailForOperation(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xem chi tiet booking.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        Booking booking = bookingDAO.getBookingById(bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking.");
        }

        return ServiceResult.success(booking, "Lay chi tiet booking thanh cong.");
    }

    /*
     * Nhân viên tạo thông tin QR chuyển khoản cho booking PENDING.
     *
     * Lưu ý:
     * - Không lưu QR vào SQL vì database hiện chưa có bảng/cột riêng cho QR.
     * - Hàm này trả về QR URL/payload để FE hiển thị.
     * - Booking vẫn chưa chuyển PAID cho tới khi STAFF/MANAGER xác nhận đã thanh toán.
     */
    public ServiceResult<PaymentQrInfo> generateBankTransferQr(User currentUser,
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

        Booking booking = bookingDAO.getBookingById(bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi tao QR cho booking dang PENDING.");
        }

        BigDecimal payableAmount = getPayableAmount(booking);

        if (payableAmount == null) {
            return ServiceResult.fail(
                    "AMOUNT_INVALID",
                    "FinalPrice cua booking khong hop le, khong the tao QR thanh toan."
            );
        }

        String cleanBankCode = cleanString(bankCode);
        String cleanAccountNumber = cleanString(accountNumber);
        String cleanAccountName = cleanString(accountName);

        ServiceResult<Boolean> bankInfoValidation = validateBankQrInput(
                cleanBankCode,
                cleanAccountNumber,
                cleanAccountName
        );

        if (!bankInfoValidation.isSuccess()) {
            return ServiceResult.fail(bankInfoValidation.getCode(), bankInfoValidation.getMessage());
        }

        String content = buildPaymentContent(bookingId);

        PaymentQrInfo qrInfo = new PaymentQrInfo(
                bookingId,
                cleanBankCode,
                cleanAccountNumber,
                cleanAccountName,
                payableAmount,
                content,
                buildVietQrImageUrl(cleanBankCode, cleanAccountNumber, cleanAccountName, payableAmount, content),
                buildPlainQrPayload(cleanBankCode, cleanAccountNumber, cleanAccountName, payableAmount, content)
        );

        /*
         * Số tiền QR luôn lấy từ booking.FinalPrice.
         * Không nhận amount từ FE để tránh sửa tiền thấp hơn/sai tiền.
         */
        return ServiceResult.success(qrInfo, "Tao thong tin QR thanh toan thanh cong.");
    }

    /*
     * STAFF/MANAGER xác nhận khách đã thanh toán.
     * Dùng cho:
     * - Khách chuyển khoản online rồi nhân viên kiểm tra.
     * - Khách chuyển khoản offline tại quầy.
     * - Khách trả tiền mặt.
     *
     * Trigger SQL sẽ:
     * - Kiểm tra Amount khớp FinalPrice.
     * - Chuyển BOOKING sang PAID.
     * - Tạo E_TICKET.
     */
    public ServiceResult<Integer> confirmBookingPaid(User currentUser,
                                                     int bookingId,
                                                     String paymentMethod,
                                                     String transactionId) {
        if (!permissionService.canUpdateBookingOperationalStatus(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xac nhan thanh toan.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        Booking booking = bookingDAO.getBookingById(bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi xac nhan thanh toan cho booking PENDING.");
        }

        BigDecimal payableAmount = getPayableAmount(booking);

        if (payableAmount == null) {
            return ServiceResult.fail("AMOUNT_INVALID", "FinalPrice cua booking khong hop le.");
        }

        String cleanPaymentMethod = cleanString(paymentMethod);

        if (cleanPaymentMethod == null) {
            return ServiceResult.fail("PAYMENT_METHOD_EMPTY", "Phuong thuc thanh toan khong duoc de trong.");
        }

        int paymentId = paymentDAO.payBooking(
                bookingId,
                payableAmount,
                cleanPaymentMethod,
                cleanString(transactionId)
        );

        if (paymentId <= 0) {
            return ServiceResult.fail("PAYMENT_FAILED", "Xac nhan thanh toan that bai.");
        }

        return ServiceResult.success(paymentId, "Xac nhan thanh toan thanh cong.");
    }

    public ServiceResult<Integer> confirmBookingPaidByCash(User currentUser, int bookingId) {
        return confirmBookingPaid(currentUser, bookingId, DEFAULT_PAYMENT_METHOD_CASH, null);
    }

    public ServiceResult<Integer> confirmBookingPaidByBanking(User currentUser,
                                                              int bookingId,
                                                              String transactionId) {
        String cleanTransactionId = cleanString(transactionId);

        if (cleanTransactionId == null) {
            return ServiceResult.fail("TRANSACTION_ID_EMPTY", "Thanh toan chuyen khoan nen co ma giao dich.");
        }

        return confirmBookingPaid(currentUser, bookingId, DEFAULT_PAYMENT_METHOD_BANKING, cleanTransactionId);
    }

    /*
     * STAFF/MANAGER tạo yêu cầu vận chuyển cho booking hoặc schedule.
     */
    public ServiceResult<Integer> createTransportRequest(User currentUser,
                                                         int scheduleId,
                                                         Integer bookingId,
                                                         String partnerName,
                                                         String contactPhone,
                                                         String pickupLocation,
                                                         String dropoffLocation,
                                                         int passengerCount,
                                                         String note) {
        if (!permissionService.canCreateTransportRequest(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc tao yeu cau van chuyen.");
        }

        if (scheduleId <= 0) {
            return ServiceResult.fail("SCHEDULE_INVALID", "ScheduleID khong hop le.");
        }

        if (passengerCount <= 0) {
            return ServiceResult.fail("PASSENGER_INVALID", "So luong khach phai lon hon 0.");
        }

        int safeBookingId = bookingId == null ? 0 : bookingId;

        int requestId = transportRequestDAO.createTransportRequest(
                scheduleId,
                safeBookingId <= 0 ? null : safeBookingId,
                cleanString(partnerName),
                cleanString(contactPhone),
                cleanString(pickupLocation),
                cleanString(dropoffLocation),
                passengerCount,
                cleanString(note),
                currentUser == null ? null : currentUser.getUserId()
        );

        if (requestId <= 0) {
            return ServiceResult.fail("TRANSPORT_CREATE_FAILED", "Tao yeu cau van chuyen that bai.");
        }

        return ServiceResult.success(requestId, "Tao yeu cau van chuyen thanh cong.");
    }

    public ServiceResult<TransportRequest> getTransportRequestDetail(User currentUser, int transportRequestId) {
        if (!permissionService.canViewOperationalBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xem yeu cau van chuyen.");
        }

        if (transportRequestId <= 0) {
            return ServiceResult.fail("TRANSPORT_INVALID", "TransportRequestID khong hop le.");
        }

        TransportRequest request = transportRequestDAO.getTransportRequestById(transportRequestId);

        if (request == null) {
            return ServiceResult.fail("TRANSPORT_NOT_FOUND", "Khong tim thay yeu cau van chuyen.");
        }

        return ServiceResult.success(request, "Lay yeu cau van chuyen thanh cong.");
    }

    public ServiceResult<List<TransportRequest>> getTransportRequestsByBooking(User currentUser, int bookingId) {
        if (!permissionService.canViewOperationalBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xem yeu cau van chuyen.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        return ServiceResult.success(
                transportRequestDAO.getTransportRequestsByBookingId(bookingId),
                "Lay danh sach yeu cau van chuyen theo booking thanh cong."
        );
    }

    public ServiceResult<List<TransportRequest>> getTransportRequestsByStatus(User currentUser, String status) {
        if (!permissionService.canViewOperationalBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xem yeu cau van chuyen.");
        }

        String cleanStatus = cleanString(status);

        if (cleanStatus == null) {
            return ServiceResult.fail("STATUS_EMPTY", "Status khong duoc de trong.");
        }

        return ServiceResult.success(
                transportRequestDAO.getTransportRequestsByStatus(cleanStatus),
                "Lay danh sach yeu cau van chuyen theo status thanh cong."
        );
    }

    public ServiceResult<Boolean> updateTransportPartner(User currentUser,
                                                         int transportRequestId,
                                                         String partnerName,
                                                         String contactPhone,
                                                         String note) {
        if (!permissionService.canUpdateTransportRequest(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc cap nhat van chuyen.");
        }

        if (transportRequestId <= 0) {
            return ServiceResult.fail("TRANSPORT_INVALID", "TransportRequestID khong hop le.");
        }

        boolean updated = transportRequestDAO.updatePartnerInfo(
                transportRequestId,
                cleanString(partnerName),
                cleanString(contactPhone),
                cleanString(note)
        );

        if (!updated) {
            return ServiceResult.fail("TRANSPORT_UPDATE_FAILED", "Cap nhat doi tac van chuyen that bai.");
        }

        return ServiceResult.success(true, "Cap nhat doi tac van chuyen thanh cong.");
    }

    public ServiceResult<Boolean> updateTransportPickupDropoff(User currentUser,
                                                               int transportRequestId,
                                                               String pickupLocation,
                                                               String dropoffLocation) {
        if (!permissionService.canUpdateTransportRequest(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc cap nhat van chuyen.");
        }

        if (transportRequestId <= 0) {
            return ServiceResult.fail("TRANSPORT_INVALID", "TransportRequestID khong hop le.");
        }

        boolean updated = transportRequestDAO.updatePickupDropoff(
                transportRequestId,
                cleanString(pickupLocation),
                cleanString(dropoffLocation)
        );

        if (!updated) {
            return ServiceResult.fail("TRANSPORT_UPDATE_FAILED", "Cap nhat diem don/tra that bai.");
        }

        return ServiceResult.success(true, "Cap nhat diem don/tra thanh cong.");
    }

    public ServiceResult<Boolean> updateTransportStatus(User currentUser,
                                                        int transportRequestId,
                                                        String status) {
        if (!permissionService.canUpdateTransportRequest(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc cap nhat van chuyen.");
        }

        if (transportRequestId <= 0) {
            return ServiceResult.fail("TRANSPORT_INVALID", "TransportRequestID khong hop le.");
        }

        String cleanStatus = cleanString(status);

        if (cleanStatus == null) {
            return ServiceResult.fail("STATUS_EMPTY", "Status khong duoc de trong.");
        }

        boolean updated = transportRequestDAO.updateTransportRequestStatus(transportRequestId, cleanStatus);

        if (!updated) {
            return ServiceResult.fail("TRANSPORT_STATUS_UPDATE_FAILED", "Cap nhat trang thai van chuyen that bai.");
        }

        return ServiceResult.success(true, "Cap nhat trang thai van chuyen thanh cong.");
    }

    /*
     * STAFF/MANAGER hỗ trợ hủy booking.
     * Với trường hợp khách tự yêu cầu, CustomerService cũng có requestCancelBooking.
     * Hàm này dùng khi nhân viên xử lý nghiệp vụ từ phía công ty hoặc hỗ trợ khách.
     */
    public ServiceResult<Integer> cancelBookingByCompany(User currentUser,
                                                         int bookingId,
                                                         String reason,
                                                         BigDecimal refundPercent) {
        if (!permissionService.canSupportBookingCancellation(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc ho tro huy booking.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        if (!bookingCancellationDAO.canCancelBooking(bookingId)) {
            return ServiceResult.fail("CANCEL_NOT_ALLOWED", "Booking nay khong du dieu kien huy.");
        }

        String cleanReason = cleanString(reason);

        if (cleanReason == null) {
            return ServiceResult.fail("CANCEL_REASON_EMPTY", "Ly do huy khong duoc de trong.");
        }

        if (refundPercent == null
                || refundPercent.compareTo(BigDecimal.ZERO) < 0
                || refundPercent.compareTo(new BigDecimal("100")) > 0) {
            return ServiceResult.fail("REFUND_PERCENT_INVALID", "RefundPercent phai tu 0 den 100.");
        }

        int bookingCancelId = bookingCancellationDAO.cancelBooking(
                bookingId,
                CANCEL_BY_COMPANY,
                cleanReason,
                refundPercent
        );

        if (bookingCancelId <= 0) {
            return ServiceResult.fail("CANCEL_FAILED", "Huy booking that bai.");
        }

        return ServiceResult.success(bookingCancelId, "Huy booking thanh cong.");
    }

    /*
     * STAFF tạo yêu cầu refund PENDING.
     * MANAGER sẽ là người complete refund ở ManagerService sau này.
     */
    public ServiceResult<Integer> createRefundRequest(User currentUser,
                                                      int bookingId,
                                                      BigDecimal refundAmount,
                                                      String refundMethod,
                                                      String transactionId) {
        if (!permissionService.canCreateRefundRequest(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc tao yeu cau refund.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ServiceResult.fail("REFUND_AMOUNT_INVALID", "So tien refund phai lon hon 0.");
        }

        String cleanRefundMethod = cleanString(refundMethod);

        if (cleanRefundMethod == null) {
            return ServiceResult.fail("REFUND_METHOD_EMPTY", "Phuong thuc refund khong duoc de trong.");
        }

        if (!refundDAO.canCreateRefund(bookingId, refundAmount)) {
            return ServiceResult.fail("REFUND_NOT_ALLOWED", "Booking khong du dieu kien hoac so tien refund khong hop le.");
        }

        int refundId = refundDAO.createRefundRequest(
                bookingId,
                normalizeMoney(refundAmount),
                cleanRefundMethod,
                cleanString(transactionId)
        );

        if (refundId <= 0) {
            return ServiceResult.fail("REFUND_CREATE_FAILED", "Tao yeu cau refund that bai.");
        }

        return ServiceResult.success(refundId, "Tao yeu cau refund thanh cong.");
    }

    public ServiceResult<Payment> getPaymentDetail(User currentUser, int paymentId) {
        if (!permissionService.canViewOperationalBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xem payment.");
        }

        if (paymentId <= 0) {
            return ServiceResult.fail("PAYMENT_INVALID", "PaymentID khong hop le.");
        }

        Payment payment = paymentDAO.getPaymentById(paymentId);

        if (payment == null) {
            return ServiceResult.fail("PAYMENT_NOT_FOUND", "Khong tim thay payment.");
        }

        return ServiceResult.success(payment, "Lay payment thanh cong.");
    }

    public ServiceResult<Refund> getRefundDetail(User currentUser, int refundId) {
        if (!permissionService.canViewOperationalBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi nhan vien hoac truong phong moi duoc xem refund.");
        }

        if (refundId <= 0) {
            return ServiceResult.fail("REFUND_INVALID", "RefundID khong hop le.");
        }

        Refund refund = refundDAO.getRefundById(refundId);

        if (refund == null) {
            return ServiceResult.fail("REFUND_NOT_FOUND", "Khong tim thay refund.");
        }

        return ServiceResult.success(refund, "Lay refund thanh cong.");
    }


    private ServiceResult<Boolean> validateBankQrInput(String bankCode,
                                                       String accountNumber,
                                                       String accountName) {
        if (bankCode == null) {
            return ServiceResult.fail("BANK_CODE_EMPTY", "Ma ngan hang khong duoc de trong.");
        }

        if (accountNumber == null) {
            return ServiceResult.fail("ACCOUNT_NUMBER_EMPTY", "So tai khoan khong duoc de trong.");
        }

        if (accountName == null) {
            return ServiceResult.fail("ACCOUNT_NAME_EMPTY", "Ten chu tai khoan khong duoc de trong.");
        }

        if (bankCode.length() > MAX_BANK_CODE_LENGTH) {
            return ServiceResult.fail("BANK_CODE_TOO_LONG", "Ma ngan hang qua dai.");
        }

        if (accountNumber.length() > MAX_ACCOUNT_NUMBER_LENGTH) {
            return ServiceResult.fail("ACCOUNT_NUMBER_TOO_LONG", "So tai khoan qua dai.");
        }

        if (accountName.length() > MAX_ACCOUNT_NAME_LENGTH) {
            return ServiceResult.fail("ACCOUNT_NAME_TOO_LONG", "Ten chu tai khoan qua dai.");
        }

        if (!bankCode.matches("^[A-Za-z0-9_\\-]+$")) {
            return ServiceResult.fail("BANK_CODE_INVALID", "Ma ngan hang chi nen gom chu, so, dau gach ngang/gach duoi.");
        }

        if (!accountNumber.matches("^[0-9]{4,30}$")) {
            return ServiceResult.fail("ACCOUNT_NUMBER_INVALID", "So tai khoan chi duoc gom 4 den 30 chu so.");
        }

        return ServiceResult.success(true, "Thong tin ngan hang hop le.");
    }

    private BigDecimal getPayableAmount(Booking booking) {
        if (booking == null || booking.getFinalPrice() == null) {
            return null;
        }

        BigDecimal finalPrice = normalizeMoney(booking.getFinalPrice());

        if (finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return finalPrice;
    }

    private boolean isPendingBooking(Booking booking) {
        return booking != null
                && booking.getStatus() != null
                && STATUS_PENDING.equalsIgnoreCase(String.valueOf(booking.getStatus()));
    }

    @SuppressWarnings("unused")
    private boolean isPaidBooking(Booking booking) {
        return booking != null
                && booking.getStatus() != null
                && STATUS_PAID.equalsIgnoreCase(String.valueOf(booking.getStatus()));
    }

    private String buildPaymentContent(int bookingId) {
        String content = "BOOKING_" + bookingId;

        if (content.length() > MAX_TRANSFER_CONTENT_LENGTH) {
            return content.substring(0, MAX_TRANSFER_CONTENT_LENGTH);
        }

        return content;
    }

    private String buildVietQrImageUrl(String bankCode,
                                       String accountNumber,
                                       String accountName,
                                       BigDecimal amount,
                                       String content) {
        String encodedAccountName = encode(accountName);
        String encodedContent = encode(content);

        return "https://img.vietqr.io/image/"
                + bankCode
                + "-"
                + accountNumber
                + "-compact2.png"
                + "?amount=" + amount.setScale(0, RoundingMode.HALF_UP)
                + "&addInfo=" + encodedContent
                + "&accountName=" + encodedAccountName;
    }

    private String buildPlainQrPayload(String bankCode,
                                       String accountNumber,
                                       String accountName,
                                       BigDecimal amount,
                                       String content) {
        return "BANK=" + bankCode
                + ";ACCOUNT=" + accountNumber
                + ";NAME=" + accountName
                + ";AMOUNT=" + amount.setScale(0, RoundingMode.HALF_UP)
                + ";CONTENT=" + content;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }

        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class PaymentQrInfo {
        private final int bookingId;
        private final String bankCode;
        private final String accountNumber;
        private final String accountName;
        private final BigDecimal amount;
        private final String transferContent;
        private final String qrImageUrl;
        private final String qrPayload;

        public PaymentQrInfo(int bookingId,
                             String bankCode,
                             String accountNumber,
                             String accountName,
                             BigDecimal amount,
                             String transferContent,
                             String qrImageUrl,
                             String qrPayload) {
            this.bookingId = bookingId;
            this.bankCode = bankCode;
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.amount = amount;
            this.transferContent = transferContent;
            this.qrImageUrl = qrImageUrl;
            this.qrPayload = qrPayload;
        }

        public int getBookingId() {
            return bookingId;
        }

        public String getBankCode() {
            return bankCode;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getAccountName() {
            return accountName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getTransferContent() {
            return transferContent;
        }

        public String getQrImageUrl() {
            return qrImageUrl;
        }

        public String getQrPayload() {
            return qrPayload;
        }

        @Override
        public String toString() {
            return "PaymentQrInfo{" +
                    "bookingId=" + bookingId +
                    ", bankCode='" + bankCode + '\'' +
                    ", accountNumber='" + accountNumber + '\'' +
                    ", accountName='" + accountName + '\'' +
                    ", amount=" + amount +
                    ", transferContent='" + transferContent + '\'' +
                    ", qrImageUrl='" + qrImageUrl + '\'' +
                    ", qrPayload='" + qrPayload + '\'' +
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
