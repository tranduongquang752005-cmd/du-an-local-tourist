package service;

import dao.BookingAddOnDAO;
import dao.BookingCancellationDAO;
import dao.BookingDAO;
import dao.CouponDAO;
import dao.ETicketDAO;
import dao.PaymentDAO;
import dao.ReviewDAO;
import dao.TourDAO;
import dao.TourImageDAO;
import dao.TourScheduleDAO;

import model.Booking;
import model.ETicket;
import model.FeaturedTourView;
import model.PopularTourView;
import model.Review;
import model.Tour;
import model.TourImage;
import model.TourSchedule;
import model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

public class CustomerService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String CANCEL_BY_CUSTOMER = "CUSTOMER";

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_POPULAR_LIMIT = 6;
    private static final int MAX_POPULAR_LIMIT = 50;

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_COUPON_CODE_LENGTH = 50;
    private static final int MAX_PAYMENT_METHOD_LENGTH = 50;
    private static final int MAX_TRANSACTION_ID_LENGTH = 100;
    private static final int MAX_CANCEL_REASON_LENGTH = 500;

    private static final int MAX_TOTAL_PASSENGERS_PER_BOOKING = 30;
    private static final int MAX_ADD_ON_QUANTITY = 20;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PermissionService permissionService;
    private final TourDAO tourDAO;
    private final TourScheduleDAO tourScheduleDAO;
    private final TourImageDAO tourImageDAO;
    private final ReviewDAO reviewDAO;
    private final BookingDAO bookingDAO;
    private final BookingAddOnDAO bookingAddOnDAO;
    private final CouponDAO couponDAO;
    private final PaymentDAO paymentDAO;
    private final ETicketDAO eTicketDAO;
    private final BookingCancellationDAO bookingCancellationDAO;

    public CustomerService() {
        this.permissionService = new PermissionService();
        this.tourDAO = new TourDAO();
        this.tourScheduleDAO = new TourScheduleDAO();
        this.tourImageDAO = new TourImageDAO();
        this.reviewDAO = new ReviewDAO();
        this.bookingDAO = new BookingDAO();
        this.bookingAddOnDAO = new BookingAddOnDAO();
        this.couponDAO = new CouponDAO();
        this.paymentDAO = new PaymentDAO();
        this.eTicketDAO = new ETicketDAO();
        this.bookingCancellationDAO = new BookingCancellationDAO();
    }

    /*
     * CUSTOMER/public:
     * Không yêu cầu đăng nhập để xem/tìm tour.
     */
    public ServiceResult<List<Tour>> getActiveTours(User currentUser) {
        if (!permissionService.canViewPublicTours(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem danh sach tour.");
        }

        return safeExecute(
                tourDAO::getAllActiveTours,
                "Lay danh sach tour thanh cong.",
                "TOUR_LIST_FAILED",
                "Lay danh sach tour that bai."
        );
    }

    public ServiceResult<List<Tour>> getToursPaging(User currentUser, int page, int pageSize) {
        if (!permissionService.canViewPublicTours(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem danh sach tour.");
        }

        int safePage = page <= 0 ? DEFAULT_PAGE : page;
        int safePageSize = normalizeLimit(pageSize, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);

        return safeExecute(
                () -> tourDAO.getToursPaging(safePage, safePageSize),
                "Lay danh sach tour phan trang thanh cong.",
                "TOUR_PAGING_FAILED",
                "Lay danh sach tour phan trang that bai."
        );
    }

    public ServiceResult<List<Tour>> searchTours(User currentUser, String keyword) {
        if (!permissionService.canSearchTours(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen tim kiem tour.");
        }

        String cleanKeyword = cleanString(keyword);

        if (!isValidText(cleanKeyword, MAX_KEYWORD_LENGTH)) {
            return ServiceResult.fail("KEYWORD_INVALID", "Tu khoa tim kiem khong hop le.");
        }

        return safeExecute(
                () -> tourDAO.searchTours(cleanKeyword),
                "Tim kiem tour thanh cong.",
                "TOUR_SEARCH_FAILED",
                "Tim kiem tour that bai."
        );
    }

    public ServiceResult<List<Tour>> searchToursForHome(User currentUser,
                                                        String keyword,
                                                        LocalDate scheduleDate,
                                                        int passengerCount) {
        if (!permissionService.canSearchTours(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen tim kiem tour.");
        }

        String cleanKeyword = cleanString(keyword);

        if (!isValidText(cleanKeyword, MAX_KEYWORD_LENGTH)) {
            return ServiceResult.fail("KEYWORD_INVALID", "Tu khoa tim kiem khong hop le.");
        }

        if (passengerCount <= 0 || passengerCount > MAX_TOTAL_PASSENGERS_PER_BOOKING) {
            return ServiceResult.fail("PASSENGER_INVALID", "So luong khach khong hop le.");
        }

        return safeExecute(
                () -> tourDAO.searchToursForHome(cleanKeyword, scheduleDate, passengerCount),
                "Tim kiem trang chu thanh cong.",
                "HOME_SEARCH_FAILED",
                "Tim kiem trang chu that bai."
        );
    }

    public ServiceResult<List<FeaturedTourView>> getFeaturedTours(User currentUser) {
        if (!permissionService.canViewPublicTours(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem tour noi bat.");
        }

        return safeExecute(
                tourDAO::getFeaturedTours,
                "Lay tour noi bat thanh cong.",
                "FEATURED_TOUR_FAILED",
                "Lay tour noi bat that bai."
        );
    }

    public ServiceResult<List<PopularTourView>> getPopularTours(User currentUser, int limit) {
        if (!permissionService.canViewPublicTours(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem tour pho bien.");
        }

        int safeLimit = normalizeLimit(limit, DEFAULT_POPULAR_LIMIT, MAX_POPULAR_LIMIT);

        return safeExecute(
                () -> tourDAO.getPopularTours(safeLimit),
                "Lay tour pho bien thanh cong.",
                "POPULAR_TOUR_FAILED",
                "Lay tour pho bien that bai."
        );
    }

    public ServiceResult<Tour> getTourDetail(User currentUser, int tourId) {
        if (!permissionService.canViewTourBasicDetail(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem chi tiet tour.");
        }

        if (tourId <= 0) {
            return ServiceResult.fail("TOUR_INVALID", "TourID khong hop le.");
        }

        return safeExecuteWithNullCheck(
                () -> {
                    Tour tour = tourDAO.getTourById(tourId);

                    if (tour == null || !tour.isActive()) {
                        return null;
                    }

                    return tour;
                },
                "Lay chi tiet tour thanh cong.",
                "TOUR_NOT_FOUND",
                "Khong tim thay tour dang hoat dong.",
                "TOUR_DETAIL_FAILED",
                "Lay chi tiet tour that bai."
        );
    }

    public ServiceResult<List<TourImage>> getTourImages(User currentUser, int tourId) {
        if (!permissionService.canViewTourImages(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem hinh anh tour.");
        }

        if (tourId <= 0) {
            return ServiceResult.fail("TOUR_INVALID", "TourID khong hop le.");
        }

        return safeExecute(
                () -> tourImageDAO.getImagesByTourId(tourId),
                "Lay hinh anh tour thanh cong.",
                "TOUR_IMAGE_FAILED",
                "Lay hinh anh tour that bai."
        );
    }

    /*
     * Theo yêu cầu hiện tại: CUSTOMER không xem itinerary/timeline chi tiết trong service này.
     * Vì vậy CustomerService không gọi TourItineraryDAO.
     */
    public ServiceResult<List<TourSchedule>> getAvailableSchedules(User currentUser, int tourId) {
        if (!permissionService.canViewTourBasicDetail(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem lich khoi hanh.");
        }

        if (tourId <= 0) {
            return ServiceResult.fail("TOUR_INVALID", "TourID khong hop le.");
        }

        return safeExecute(
                () -> tourScheduleDAO.getAvailableSchedulesByTourId(tourId),
                "Lay lich khoi hanh con cho thanh cong.",
                "SCHEDULE_LIST_FAILED",
                "Lay lich khoi hanh that bai."
        );
    }

    public ServiceResult<List<Review>> getTourReviews(User currentUser, int tourId) {
        if (!permissionService.canViewTourReviews(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem danh gia tour.");
        }

        if (tourId <= 0) {
            return ServiceResult.fail("TOUR_INVALID", "TourID khong hop le.");
        }

        return safeExecute(
                () -> reviewDAO.getReviewsByTourId(tourId),
                "Lay danh gia tour thanh cong.",
                "REVIEW_LIST_FAILED",
                "Lay danh gia tour that bai."
        );
    }

    public ServiceResult<BigDecimal> getAverageRating(User currentUser, int tourId) {
        if (!permissionService.canViewTourReviews(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Khong co quyen xem diem danh gia.");
        }

        if (tourId <= 0) {
            return ServiceResult.fail("TOUR_INVALID", "TourID khong hop le.");
        }

        return safeExecute(
                () -> reviewDAO.getAverageRatingByTourId(tourId),
                "Lay diem danh gia thanh cong.",
                "AVERAGE_RATING_FAILED",
                "Lay diem danh gia that bai."
        );
    }

    /*
     * CUSTOMER booking:
     * Chỉ CUSTOMER active mới được tạo booking.
     */
    public ServiceResult<Integer> createBooking(User currentUser,
                                                int tourId,
                                                int scheduleId,
                                                int adultCount,
                                                int childCount,
                                                int babyCount,
                                                String couponCode) {
        if (!permissionService.canCreateBooking(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi khach hang moi duoc dat tour.");
        }

        BookingInputValidation validation = validateBookingInput(
                tourId,
                scheduleId,
                adultCount,
                childCount,
                babyCount,
                couponCode
        );

        if (!validation.valid) {
            return ServiceResult.fail(validation.code, validation.message);
        }

        boolean enoughSlots;

        try {
            enoughSlots = tourScheduleDAO.hasEnoughSlots(scheduleId, adultCount, childCount);
        } catch (Exception e) {
            return ServiceResult.fail("SLOT_CHECK_FAILED", "Kiem tra ghe that bai: " + safeExceptionMessage(e));
        }

        if (!enoughSlots) {
            return ServiceResult.fail("NOT_ENOUGH_SLOTS", "Lich khoi hanh khong du cho trong.");
        }

        int bookingId;

        try {
            bookingId = bookingDAO.createBooking(
                    currentUser.getUserId(),
                    tourId,
                    scheduleId,
                    adultCount,
                    childCount,
                    babyCount,
                    validation.couponCode
            );
        } catch (Exception e) {
            return ServiceResult.fail("BOOKING_EXCEPTION", "Tao booking that bai: " + safeExceptionMessage(e));
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_FAILED", "Tao booking that bai.");
        }

        return ServiceResult.success(bookingId, "Tao booking thanh cong.");
    }

    public ServiceResult<Booking> getMyBookingDetail(User currentUser, int bookingId) {
        if (!permissionService.canViewOwnBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi khach hang moi duoc xem booking cua minh.");
        }

        if (bookingId <= 0) {
            return ServiceResult.fail("BOOKING_INVALID", "BookingID khong hop le.");
        }

        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        return ServiceResult.success(booking, "Lay booking thanh cong.");
    }

    public ServiceResult<Boolean> addAddOnToPendingBooking(User currentUser,
                                                           int bookingId,
                                                           int addOnId,
                                                           int quantity) {
        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi duoc them add-on khi booking dang PENDING.");
        }

        if (addOnId <= 0) {
            return ServiceResult.fail("ADDON_INVALID", "AddOnID khong hop le.");
        }

        if (quantity <= 0 || quantity > MAX_ADD_ON_QUANTITY) {
            return ServiceResult.fail("ADDON_QUANTITY_INVALID", "So luong add-on khong hop le.");
        }

        int bookingAddOnId;

        try {
            bookingAddOnId = bookingAddOnDAO.addBookingAddOn(bookingId, addOnId, quantity);
        } catch (Exception e) {
            return ServiceResult.fail("ADDON_EXCEPTION", "Them add-on that bai: " + safeExceptionMessage(e));
        }

        if (bookingAddOnId <= 0) {
            return ServiceResult.fail("ADDON_FAILED", "Them add-on vao booking that bai.");
        }

        return ServiceResult.success(true, "Them add-on thanh cong.");
    }

    public ServiceResult<Boolean> updateAddOnQuantity(User currentUser,
                                                      int bookingId,
                                                      int bookingAddOnId,
                                                      int quantity) {
        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi duoc sua add-on khi booking dang PENDING.");
        }

        if (bookingAddOnId <= 0) {
            return ServiceResult.fail("BOOKING_ADDON_INVALID", "BookingAddOnID khong hop le.");
        }

        if (quantity <= 0 || quantity > MAX_ADD_ON_QUANTITY) {
            return ServiceResult.fail("ADDON_QUANTITY_INVALID", "So luong add-on khong hop le.");
        }

        boolean updated;

        try {
            updated = bookingAddOnDAO.updateQuantity(bookingAddOnId, quantity);
        } catch (Exception e) {
            return ServiceResult.fail("ADDON_UPDATE_EXCEPTION", "Cap nhat add-on that bai: " + safeExceptionMessage(e));
        }

        if (!updated) {
            return ServiceResult.fail("ADDON_UPDATE_FAILED", "Cap nhat so luong add-on that bai.");
        }

        return ServiceResult.success(true, "Cap nhat add-on thanh cong.");
    }

    public ServiceResult<CouponDAO.CouponCheckResult> checkCoupon(User currentUser,
                                                                  int bookingId,
                                                                  String couponCode) {
        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi kiem tra coupon khi booking dang PENDING.");
        }

        String cleanCouponCode = normalizeCouponCode(couponCode);

        if (cleanCouponCode == null) {
            return ServiceResult.fail("COUPON_INVALID", "Ma coupon khong hop le.");
        }

        return safeExecute(
                () -> couponDAO.checkCouponForUser(
                        cleanCouponCode,
                        currentUser.getUserId(),
                        booking.getTotalPrice()
                ),
                "Kiem tra coupon thanh cong.",
                "COUPON_CHECK_FAILED",
                "Kiem tra coupon that bai."
        );
    }

    public ServiceResult<Boolean> applyCouponToPendingBooking(User currentUser,
                                                              int bookingId,
                                                              String couponCode) {
        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi duoc ap coupon khi booking dang PENDING.");
        }

        String cleanCouponCode = normalizeCouponCode(couponCode);

        if (cleanCouponCode == null) {
            return ServiceResult.fail("COUPON_INVALID", "Ma coupon khong hop le.");
        }

        boolean applied;

        try {
            applied = couponDAO.applyCouponToPendingBooking(bookingId, cleanCouponCode);
        } catch (Exception e) {
            return ServiceResult.fail("COUPON_APPLY_EXCEPTION", "Ap coupon that bai: " + safeExceptionMessage(e));
        }

        if (!applied) {
            return ServiceResult.fail("COUPON_APPLY_FAILED", "Ap coupon that bai.");
        }

        return ServiceResult.success(true, "Ap coupon thanh cong.");
    }

    /*
     * CUSTOMER thanh toán booking PENDING.
     *
     * Ghi chú nghiệp vụ:
     * - Hàm này chỉ dùng khi hệ thống đã xác nhận thanh toán thành công từ phía khách
     *   như cổng thanh toán online hoặc luồng FE đã xác nhận xong.
     * - Không tạo QR tại đây.
     * - QR chuyển khoản do STAFF/MANAGER xử lý ở StaffService.
     * - PaymentMethod không ép cứng vì SQL hiện tại không có CHECK cho PaymentMethod.
     */
    public ServiceResult<Boolean> payPendingBooking(User currentUser,
                                                    int bookingId,
                                                    BigDecimal amount,
                                                    String paymentMethod,
                                                    String transactionId) {
        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        if (!isPendingBooking(booking)) {
            return ServiceResult.fail("BOOKING_NOT_PENDING", "Chi duoc thanh toan booking PENDING.");
        }

        if (amount == null || amount.compareTo(ZERO) <= 0) {
            return ServiceResult.fail("PAYMENT_AMOUNT_INVALID", "So tien thanh toan khong hop le.");
        }

        BigDecimal finalPrice = booking.getFinalPrice();

        if (finalPrice == null || amount.compareTo(finalPrice) != 0) {
            return ServiceResult.fail("PAYMENT_AMOUNT_MISMATCH", "So tien thanh toan phai bang FinalPrice.");
        }

        String cleanPaymentMethod = cleanString(paymentMethod);

        if (cleanPaymentMethod == null || cleanPaymentMethod.length() > MAX_PAYMENT_METHOD_LENGTH) {
            return ServiceResult.fail("PAYMENT_METHOD_INVALID", "Phuong thuc thanh toan khong hop le.");
        }

        String cleanTransactionId = cleanString(transactionId);

        if (cleanTransactionId != null && cleanTransactionId.length() > MAX_TRANSACTION_ID_LENGTH) {
            return ServiceResult.fail("TRANSACTION_TOO_LONG", "TransactionID qua dai.");
        }

        int paymentId;

        try {
            paymentId = paymentDAO.payBooking(
                    bookingId,
                    amount,
                    cleanPaymentMethod,
                    cleanTransactionId
            );
        } catch (Exception e) {
            return ServiceResult.fail("PAYMENT_EXCEPTION", "Thanh toan booking that bai: " + safeExceptionMessage(e));
        }

        if (paymentId <= 0) {
            return ServiceResult.fail("PAYMENT_FAILED", "Thanh toan booking that bai.");
        }

        return ServiceResult.success(true, "Thanh toan booking thanh cong.");
    }

    public ServiceResult<ETicket> getMyTicketByBooking(User currentUser, int bookingId) {
        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        return safeExecuteWithNullCheck(
                () -> eTicketDAO.getTicketByBookingId(bookingId),
                "Lay ve dien tu thanh cong.",
                "TICKET_NOT_FOUND",
                "Booking chua co ve dien tu.",
                "TICKET_GET_FAILED",
                "Lay ve dien tu that bai."
        );
    }

    public ServiceResult<List<ETicket>> getMyTickets(User currentUser) {
        if (!permissionService.canViewOwnBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi khach hang moi duoc xem ve cua minh.");
        }

        return safeExecute(
                () -> eTicketDAO.getTicketsByUserId(currentUser.getUserId()),
                "Lay danh sach ve thanh cong.",
                "TICKET_LIST_FAILED",
                "Lay danh sach ve that bai."
        );
    }

    public ServiceResult<List<ETicket>> getMyActiveTickets(User currentUser) {
        if (!permissionService.canViewOwnBookings(currentUser)) {
            return ServiceResult.fail("NO_PERMISSION", "Chi khach hang moi duoc xem ve cua minh.");
        }

        return safeExecute(
                () -> eTicketDAO.getActiveTicketsByUserId(currentUser.getUserId()),
                "Lay danh sach ve active thanh cong.",
                "ACTIVE_TICKET_LIST_FAILED",
                "Lay danh sach ve active that bai."
        );
    }

    public ServiceResult<Boolean> requestCancelBooking(User currentUser,
                                                       int bookingId,
                                                       String reason,
                                                       BigDecimal refundPercent) {
        Booking booking = getOwnedBooking(currentUser, bookingId);

        if (booking == null) {
            return ServiceResult.fail("BOOKING_NOT_FOUND", "Khong tim thay booking cua khach hang.");
        }

        boolean canCancel;

        try {
            canCancel = bookingCancellationDAO.canCancelBooking(bookingId);
        } catch (Exception e) {
            return ServiceResult.fail("CANCEL_CHECK_FAILED", "Kiem tra huy booking that bai: " + safeExceptionMessage(e));
        }

        if (!canCancel) {
            return ServiceResult.fail("CANCEL_NOT_ALLOWED", "Booking nay khong du dieu kien huy.");
        }

        String cleanReason = cleanString(reason);

        if (!isValidText(cleanReason, MAX_CANCEL_REASON_LENGTH)) {
            return ServiceResult.fail("CANCEL_REASON_INVALID", "Ly do huy khong hop le.");
        }

        if (refundPercent == null
                || refundPercent.compareTo(ZERO) < 0
                || refundPercent.compareTo(ONE_HUNDRED) > 0) {
            return ServiceResult.fail("REFUND_PERCENT_INVALID", "RefundPercent phai tu 0 den 100.");
        }

        int bookingCancelId;

        try {
            bookingCancelId = bookingCancellationDAO.cancelBooking(
                    bookingId,
                    CANCEL_BY_CUSTOMER,
                    cleanReason,
                    refundPercent
            );
        } catch (Exception e) {
            return ServiceResult.fail("CANCEL_EXCEPTION", "Gui yeu cau huy booking that bai: " + safeExceptionMessage(e));
        }

        if (bookingCancelId <= 0) {
            return ServiceResult.fail("CANCEL_FAILED", "Gui yeu cau huy booking that bai.");
        }

        return ServiceResult.success(true, "Gui yeu cau huy booking thanh cong.");
    }

    private Booking getOwnedBooking(User currentUser, int bookingId) {
        if (!permissionService.isCustomer(currentUser) || bookingId <= 0) {
            return null;
        }

        try {
            Booking booking = bookingDAO.getBookingById(bookingId);

            if (booking == null) {
                return null;
            }

            if (booking.getUserId() != currentUser.getUserId()) {
                return null;
            }

            return booking;
        } catch (Exception e) {
            return null;
        }
    }

    private BookingInputValidation validateBookingInput(int tourId,
                                                        int scheduleId,
                                                        int adultCount,
                                                        int childCount,
                                                        int babyCount,
                                                        String couponCode) {
        if (tourId <= 0 || scheduleId <= 0) {
            return BookingInputValidation.invalid("BOOKING_INPUT_INVALID", "TourID hoac ScheduleID khong hop le.");
        }

        if (adultCount < 0 || childCount < 0 || babyCount < 0) {
            return BookingInputValidation.invalid("PASSENGER_INVALID", "So luong hanh khach khong duoc am.");
        }

        int totalPassengers = adultCount + childCount + babyCount;

        if (totalPassengers <= 0) {
            return BookingInputValidation.invalid("PASSENGER_REQUIRED", "Phai co it nhat 1 hanh khach.");
        }

        if (totalPassengers > MAX_TOTAL_PASSENGERS_PER_BOOKING) {
            return BookingInputValidation.invalid("PASSENGER_TOO_MANY", "So luong hanh khach vuot gioi han.");
        }

        String cleanCouponCode = normalizeCouponCode(couponCode);

        if (couponCode != null && cleanCouponCode == null) {
            return BookingInputValidation.invalid("COUPON_INVALID", "Ma coupon khong hop le.");
        }

        return BookingInputValidation.valid(cleanCouponCode);
    }

    private String normalizeCouponCode(String couponCode) {
        String cleanCouponCode = cleanString(couponCode);

        if (cleanCouponCode == null) {
            return null;
        }

        cleanCouponCode = cleanCouponCode.toUpperCase();

        if (cleanCouponCode.length() > MAX_COUPON_CODE_LENGTH) {
            return null;
        }

        if (!cleanCouponCode.matches("^[A-Z0-9_\\-]+$")) {
            return null;
        }

        return cleanCouponCode;
    }

    private boolean isPendingBooking(Booking booking) {
        return booking != null
                && booking.getStatus() != null
                && STATUS_PENDING.equalsIgnoreCase(String.valueOf(booking.getStatus()));
    }

    private int normalizeLimit(int value, int defaultValue, int maxValue) {
        if (value <= 0) {
            return defaultValue;
        }

        return Math.min(value, maxValue);
    }

    private boolean isValidText(String value, int maxLength) {
        return value != null && value.length() <= maxLength;
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

    private <T> ServiceResult<T> safeExecute(Supplier<T> supplier,
                                             String successMessage,
                                             String failCode,
                                             String failMessage) {
        try {
            T data = supplier.get();
            return ServiceResult.success(data, successMessage);
        } catch (Exception e) {
            return ServiceResult.fail(failCode, failMessage + ": " + safeExceptionMessage(e));
        }
    }

    private <T> ServiceResult<T> safeExecuteWithNullCheck(Supplier<T> supplier,
                                                          String successMessage,
                                                          String nullCode,
                                                          String nullMessage,
                                                          String failCode,
                                                          String failMessage) {
        try {
            T data = supplier.get();

            if (data == null) {
                return ServiceResult.fail(nullCode, nullMessage);
            }

            return ServiceResult.success(data, successMessage);
        } catch (Exception e) {
            return ServiceResult.fail(failCode, failMessage + ": " + safeExceptionMessage(e));
        }
    }

    private static class BookingInputValidation {
        private final boolean valid;
        private final String code;
        private final String message;
        private final String couponCode;

        private BookingInputValidation(boolean valid, String code, String message, String couponCode) {
            this.valid = valid;
            this.code = code;
            this.message = message;
            this.couponCode = couponCode;
        }

        private static BookingInputValidation valid(String couponCode) {
            return new BookingInputValidation(true, null, null, couponCode);
        }

        private static BookingInputValidation invalid(String code, String message) {
            return new BookingInputValidation(false, code, message, null);
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
