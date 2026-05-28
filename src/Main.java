import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import config.DatabaseConnection;

import dao.AddOnDAO;
import dao.AuditLogDAO;
import dao.BookingAddOnDAO;
import dao.BookingCancellationDAO;
import dao.BookingDAO;
import dao.BookingPassengerDAO;
import dao.CouponDAO;
import dao.DynamicPriceRuleDAO;
import dao.ETicketDAO;
import dao.FeaturedTourDAO;
import dao.FuelPriceDAO;
import dao.LocationDAO;
import dao.PaymentDAO;
import dao.RefundDAO;
import dao.RevenueDAO;
import dao.RevenueDAO.MonthlyRevenueRow;
import dao.RevenueDAO.PaymentRevenueRow;
import dao.RevenueDAO.RevenueSummary;
import dao.RevenueDAO.TourRevenueRow;
import dao.ReviewDAO;
import dao.SystemConfigDAO;
import dao.TourCategoryDAO;
import dao.TourDAO;
import dao.TourImageDAO;
import dao.TourItineraryDAO;
import dao.TourLocationDAO;
import dao.TourPriceDAO;
import dao.TourScheduleDAO;
import dao.TransportRequestDAO;
import dao.UserDAO;

import model.AddOn;
import model.Booking;
import model.BookingCancellation;
import model.BookingAddOn;
import model.Coupon;
import model.ETicket;
import model.FeaturedTourView;
import model.FuelPrice;
import model.Location;
import model.Payment;
import model.PopularTourView;
import model.SystemConfig;
import model.Tour;
import model.TourImage;
import model.TourItinerary;
import model.TourPrice;
import model.TourSchedule;
import model.TransportRequest;
import model.User;
import service.AuthService;
import util.AES256Util;
import util.PasswordUtil;

public class Main {

    private static final int TEST_USER_ID = 3;
    private static final int TEST_TOUR_ID = 1;

    public static void main(String[] args) throws Exception {
        Connection conn = DatabaseConnection.getConnection();

        if (conn != null) {
            System.out.println("Ket noi SQL Server thanh cong!");
            conn.close();
        } else {
            System.out.println("Ket noi SQL Server that bai!");
            return;
        }

        // =========================
        // TEST TOUR DAO
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER xem danh sách, tìm kiếm, tour nổi bật/phổ biến.
        // STAFF / MANAGER dùng dữ liệu này để kiểm tra tour đang hiển thị.
        // =========================
        TourDAO tourDAO = new TourDAO();

        System.out.println("\n===== DANH SACH TOUR ACTIVE =====");
        List<Tour> tours = tourDAO.getAllActiveTours();
        printList(tours);

        System.out.println("\n===== LOAD MORE / PAGING PAGE 1 =====");
        printList(tourDAO.getToursPaging(1, 3));

        System.out.println("\n===== TIM KIEM TU KHOA: Tam linh =====");
        printList(tourDAO.searchTours("Tâm linh"));

        System.out.println("\n===== TIM KIEM TRANG CHU =====");
        printList(tourDAO.searchToursForHome("Cần Thơ", LocalDate.of(2026, 7, 1), 2));

        System.out.println("\n===== TOUR NOI BAT VIEW =====");
        List<FeaturedTourView> featuredTours = tourDAO.getFeaturedTours();
        printList(featuredTours);

        System.out.println("\n===== TOUR PHO BIEN =====");
        List<PopularTourView> popularTours = tourDAO.getPopularTours(6);
        printList(popularTours);

        // =========================
        // TEST TOUR SCHEDULE DAO
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER xem lịch khởi hành và số chỗ còn lại.
        // STAFF / MANAGER quản lý lịch khởi hành.
        // =========================
        TourScheduleDAO scheduleDAO = new TourScheduleDAO();

        System.out.println("\n===== LICH KHOI HANH TOUR ID = 1 =====");
        List<TourSchedule> schedules = scheduleDAO.getSchedulesByTourId(TEST_TOUR_ID);
        printList(schedules);

        System.out.println("\n===== LICH CON CHO TOUR ID = 1 =====");
        printList(scheduleDAO.getAvailableSchedulesByTourId(TEST_TOUR_ID));

        int scheduleId = findAvailableScheduleId(schedules);

        System.out.println("\n===== KIEM TRA GHE SCHEDULE =====");
        if (scheduleId > 0) {
            boolean enoughSlots = scheduleDAO.hasEnoughSlots(scheduleId, 1, 0);
            System.out.println("ScheduleID = " + scheduleId + ", con du 1 ghe? " + enoughSlots);
        } else {
            System.out.println("Khong tim thay schedule con ghe.");
        }

        System.out.println("\n===== TIM LICH THEO TOUR + NGAY =====");
        TourSchedule selectedSchedule =
                scheduleDAO.getScheduleByTourAndDate(TEST_TOUR_ID, LocalDate.of(2026, 7, 1));
        System.out.println(selectedSchedule);

        // =========================
        // TEST TOUR IMAGE DAO
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER xem hình ảnh tour.
        // STAFF / MANAGER quản lý hình ảnh tour.
        // =========================
        TourImageDAO imageDAO = new TourImageDAO();

        System.out.println("\n===== HINH ANH TOUR ID = 1 =====");
        List<TourImage> images = imageDAO.getImagesByTourId(TEST_TOUR_ID);
        printList(images);

        System.out.println("\n===== ANH CHINH TOUR ID = 1 =====");
        TourImage mainImage = imageDAO.getMainImageByTourId(TEST_TOUR_ID);
        System.out.println(mainImage);

        // =========================
        // TEST TOUR ITINERARY DAO
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER xem lịch trình tour.
        // STAFF / MANAGER thêm/sửa/xóa lịch trình.
        // =========================
        TourItineraryDAO itineraryDAO = new TourItineraryDAO();

        System.out.println("\n===== LICH TRINH TOUR ID = 1 =====");
        List<TourItinerary> itineraries = itineraryDAO.getItineraryByTourId(TEST_TOUR_ID);
        printList(itineraries);

        System.out.println("\n===== LICH TRINH TOUR ID = 1, NGAY 1 =====");
        List<TourItinerary> dayOneItineraries =
                itineraryDAO.getItineraryByTourIdAndDay(TEST_TOUR_ID, 1);
        printList(dayOneItineraries);

        // =========================
        // TEST REVIEW DAO
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER xem/gửi đánh giá sau khi hoàn thành tour.
        // STAFF / MANAGER theo dõi chất lượng dịch vụ.
        // =========================
        ReviewDAO reviewDAO = new ReviewDAO();

        System.out.println("\n===== DANH GIA GAN DAY =====");
        printList(reviewDAO.getRecentReviews(5));

        System.out.println("\n===== DANH GIA CUA TOUR ID = 1 =====");
        printList(reviewDAO.getReviewsByTourId(TEST_TOUR_ID));

        System.out.println("\n===== DIEM TRUNG BINH TOUR ID = 1 =====");
        BigDecimal avgRating = reviewDAO.getAverageRatingByTourId(TEST_TOUR_ID);
        System.out.println("Average rating = " + avgRating);

        // =========================
        // TEST USER DAO + PASSWORD + AES
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER đăng ký/đăng nhập.
        // STAFF / MANAGER quản lý tài khoản theo quyền.
        // MANAGER được xem/quản lý dữ liệu nhạy cảm hơn.
        // =========================
        UserDAO userDAO = new UserDAO();

        System.out.println("\n===== TEST PBKDF2 PASSWORD =====");
        String hash = PasswordUtil.hashPassword("123456");
        System.out.println("Hash = " + hash);
        System.out.println("Verify dung = " + PasswordUtil.verifyPassword("123456", hash));
        System.out.println("Verify sai = " + PasswordUtil.verifyPassword("999999", hash));

        System.out.println("\n===== TEST AES-256 FULLNAME =====");
        String encryptedName = AES256Util.encrypt("Nguyễn Văn Test");
        System.out.println("Encrypted = " + encryptedName);
        System.out.println("Decrypted = " + AES256Util.decrypt(encryptedName));

        System.out.println("\n===== TEST REGISTER CUSTOMER =====");
        String testPhone = "0912345678";

        if (!userDAO.phoneExists(testPhone)) {
            int newUserId = userDAO.registerCustomer("Nguyễn Văn Test", testPhone, "123456");
            System.out.println("New user id = " + newUserId);
        } else {
            System.out.println("Phone da ton tai, bo qua register.");
        }

        System.out.println("\n===== TEST LOGIN CUSTOMER =====");
        User loginUser = userDAO.login(testPhone, "123456");
        System.out.println(loginUser);

        System.out.println("\n===== TEST DANH SACH CUSTOMER =====");
        printList(userDAO.getAllCustomers());

        // =========================
        // TEST BOOKING / PAYMENT / TICKET / TRANSPORT
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER đặt tour, thanh toán, nhận vé điện tử.
        // STAFF tiếp nhận booking và gửi yêu cầu vận chuyển cho đối tác.
        // MANAGER có quyền kiểm tra toàn bộ quy trình.
        // =========================
        BookingDAO bookingDAO = new BookingDAO();
        PaymentDAO paymentDAO = new PaymentDAO();
        ETicketDAO eTicketDAO = new ETicketDAO();
        TransportRequestDAO transportDAO = new TransportRequestDAO();

        int bookingId = -1;

        System.out.println("\n===== TEST TAO BOOKING MOI =====");

        if (scheduleId <= 0) {
            System.out.println("Khong tim thay lich con cho de test booking. Bo qua Booking/Payment/Transport.");
        } else {
            System.out.println("ScheduleID dung de test = " + scheduleId);

            bookingId = bookingDAO.createBooking(
                    TEST_USER_ID,
                    TEST_TOUR_ID,
                    scheduleId,
                    1,
                    0,
                    0,
                    null
            );

            System.out.println("BookingID moi = " + bookingId);

            if (bookingId > 0) {
                runBookingPaymentTransportTests(
                        bookingId,
                        TEST_USER_ID,
                        scheduleId,
                        bookingDAO,
                        paymentDAO,
                        eTicketDAO,
                        transportDAO
                );
            } else {
                System.out.println("Tao booking that bai, bo qua Booking/Payment/Transport.");
            }
        }

        // =========================
        // TEST BOOKING CANCELLATION DAO
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER có thể yêu cầu hủy booking.
        // STAFF / MANAGER xử lý hủy booking và tính số tiền dự kiến hoàn.
        // =========================
        BookingCancellationDAO bookingCancellationDAO = new BookingCancellationDAO();

        System.out.println("\n===== TEST BOOKING CANCELLATION =====");
        List<BookingCancellation> allCancellations = bookingCancellationDAO.getAllCancellations();
        printList(allCancellations);

        if (bookingId > 0) {
            System.out.println("\n===== TEST KIEM TRA CO THE HUY BOOKING VUA TAO =====");
            boolean canCancelBooking = bookingCancellationDAO.canCancelBooking(bookingId);
            System.out.println("Can cancel booking " + bookingId + "? " + canCancelBooking);

            BigDecimal expectedRefundAmount =
                    bookingCancellationDAO.calculateExpectedRefundAmount(
                            bookingId,
                            new BigDecimal("50")
                    );
            System.out.println("Expected refund amount 50% = " + expectedRefundAmount);

            if (canCancelBooking) {
                int bookingCancelId = bookingCancellationDAO.cancelBooking(
                        bookingId,
                        "STAFF",
                        "Khach yeu cau huy booking trong qua trinh test DAO",
                        new BigDecimal("50")
                );

                System.out.println("New BookingCancelID = " + bookingCancelId);

                if (bookingCancelId > 0) {
                    System.out.println(bookingCancellationDAO.getCancellationById(bookingCancelId));

                    boolean updateReason = bookingCancellationDAO.updateCancellationReason(
                            bookingCancelId,
                            "Cap nhat ly do huy booking trong test DAO"
                    );
                    System.out.println("Update cancellation reason = " + updateReason);

                    System.out.println(bookingCancellationDAO.getCancellationByBookingId(bookingId));
                }
            }
        }

        // =========================
        // TEST REFUND DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF tạo yêu cầu hoàn tiền cho booking đã hủy.
        // MANAGER kiểm tra toàn bộ lịch sử refund và ảnh hưởng doanh thu.
        // =========================
        RefundDAO refundDAO = new RefundDAO();

        System.out.println("\n===== TEST REFUND GAN DAY =====");
        printList(refundDAO.getRecentRefunds(10));

        if (bookingId > 0) {
            System.out.println("\n===== TEST REFUND THEO BOOKING VUA TAO =====");
            printList(refundDAO.getRefundsByBookingId(bookingId));

            System.out.println("\n===== TEST KIEM TRA SO TIEN CO THE REFUND =====");
            BigDecimal remainingRefundable = refundDAO.getRemainingRefundableAmount(bookingId);
            System.out.println("Remaining refundable amount = " + remainingRefundable);

            BigDecimal testRefundAmount = new BigDecimal("100000");

            if (refundDAO.canCreateRefund(bookingId, testRefundAmount)) {
                int newRefundId = refundDAO.createRefundRequest(
                        bookingId,
                        testRefundAmount,
                        "BANKING",
                        "REF_TEST_" + System.currentTimeMillis()
                );

                System.out.println("New RefundID = " + newRefundId);

                if (newRefundId > 0) {
                    System.out.println(refundDAO.getRefundById(newRefundId));

                    boolean completeRefund = refundDAO.completeRefund(
                            newRefundId,
                            "REF_SUCCESS_" + System.currentTimeMillis()
                    );
                    System.out.println("Complete refund SUCCESS = " + completeRefund);

                    System.out.println(refundDAO.getRefundById(newRefundId));
                }
            } else {
                System.out.println("Booking vua tao chua du dieu kien refund.");
            }
        }

        System.out.println("\n===== TEST TONG REFUND SUCCESS NAM 2026 =====");
        BigDecimal totalRefundSuccess = refundDAO.getTotalSuccessfulRefundAmount(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
        System.out.println("Total successful refund 2026 = " + totalRefundSuccess);

        // =========================
        // TEST REVENUE DAO
        // Chức năng này dành cho MANAGER / ADMIN
        // STAFF không nên xem full doanh thu/lợi nhuận.
        // MANAGER được xem tổng doanh thu, doanh thu theo tour, doanh thu theo tháng.
        // =========================
        RevenueDAO revenueDAO = new RevenueDAO();

        System.out.println("\n===== TEST REVENUE SUMMARY =====");
        RevenueSummary summary = revenueDAO.getRevenueSummary(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
        System.out.println(summary);

        System.out.println("\n===== TEST REVENUE BY TOUR =====");
        List<TourRevenueRow> tourRevenueRows = revenueDAO.getRevenueByTour(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
        printList(tourRevenueRows);

        System.out.println("\n===== TEST MONTHLY REVENUE =====");
        List<MonthlyRevenueRow> monthlyRows = revenueDAO.getMonthlyRevenue(2026);
        printList(monthlyRows);

        System.out.println("\n===== TEST RECENT SUCCESS PAYMENTS =====");
        List<PaymentRevenueRow> recentPayments = revenueDAO.getRecentSuccessfulPayments(10);
        printList(recentPayments);

        // =========================
        // TEST AUDIT LOG DAO
        // Chức năng này dành cho MANAGER / ADMIN
        // Dùng để kiểm tra lịch sử thay đổi trạng thái booking và dữ liệu quan trọng.
        // =========================
        AuditLogDAO auditLogDAO = new AuditLogDAO();

        System.out.println("\n===== TEST AUDIT LOG GAN DAY =====");
        printList(auditLogDAO.getRecentAuditLogs(20));

        if (bookingId > 0) {
            System.out.println("\n===== TEST AUDIT LOG CUA BOOKING VUA TAO =====");
            printList(auditLogDAO.getBookingAuditLogs(bookingId));
        }

        System.out.println("\n===== TEST AUDIT LOG THEO BANG BOOKINGS =====");
        printList(auditLogDAO.getAuditLogsByTable("BOOKINGS", 20));

        System.out.println("\n===== COUNT AUDIT LOG BOOKINGS =====");
        int bookingAuditCount = auditLogDAO.countAuditLogsByTable("BOOKINGS");
        System.out.println("Booking audit count = " + bookingAuditCount);

        // =========================
        // TEST SYSTEM CONFIG DAO
        // Chức năng này dành cho MANAGER / ADMIN
        // Dùng để đọc/cập nhật cấu hình hệ thống.
        // =========================
        SystemConfigDAO systemConfigDAO = new SystemConfigDAO();

        System.out.println("\n===== TEST SYSTEM CONFIG BY KEY =====");
        SystemConfig maxPendingConfig = systemConfigDAO.getConfigByKey("booking.max_pending_per_user");
        System.out.println(maxPendingConfig);

        System.out.println("\n===== TEST GET INT CONFIG =====");
        int maxPending = systemConfigDAO.getIntValue("booking.max_pending_per_user", 3);
        System.out.println("Max pending booking = " + maxPending);

        System.out.println("\n===== TEST GET BOOLEAN CONFIG =====");
        boolean degradedMode = systemConfigDAO.getBooleanValue("traffic.degraded_mode", false);
        System.out.println("Traffic degraded mode = " + degradedMode);

        System.out.println("\n===== TEST UPDATE CONFIG VALUE =====");
        boolean updateConfig = systemConfigDAO.updateConfigValue("booking.max_pending_per_user", "3", 1);
        System.out.println("Update config = " + updateConfig);

        System.out.println("\n===== TEST ACTIVE SYSTEM CONFIGS =====");
        printList(systemConfigDAO.getActiveConfigs());

        // =========================
        // TEST FUEL PRICE DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF cập nhật giá xăng phục vụ tính phụ thu/lịch khởi hành.
        // MANAGER kiểm tra và quản lý lịch sử giá.
        // =========================
        FuelPriceDAO fuelPriceDAO = new FuelPriceDAO();

        System.out.println("\n===== TEST DANH SACH FUEL PRICE =====");
        printList(fuelPriceDAO.getAllFuelPrices());

        System.out.println("\n===== TEST LATEST FUEL PRICE =====");
        FuelPrice latestFuelPrice = fuelPriceDAO.getLatestFuelPrice();
        System.out.println(latestFuelPrice);

        System.out.println("\n===== TEST FUEL PRICE GAN NHAT THEO NGAY =====");
        FuelPrice fuelPriceByDate =
                fuelPriceDAO.getLatestFuelPriceBeforeOrOn(LocalDate.of(2026, 7, 8));
        System.out.println(fuelPriceByDate);

        // =========================
        // TEST TOUR CATEGORY DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF / MANAGER thêm, sửa, ẩn/xóa danh mục tour khi phù hợp nghiệp vụ.
        // =========================
        TourCategoryDAO categoryDAO = new TourCategoryDAO();

        System.out.println("\n===== TEST DANH SACH CATEGORY ACTIVE =====");
        printList(categoryDAO.getActiveCategories());

        System.out.println("\n===== TEST THEM CATEGORY MOI =====");
        String testCategoryName = "Tour test " + System.currentTimeMillis();
        int newCategoryId = categoryDAO.createCategory(testCategoryName, "Danh muc dung de test DAO");
        System.out.println("New CategoryID = " + newCategoryId);

        if (newCategoryId > 0) {
            System.out.println(categoryDAO.getCategoryById(newCategoryId));

            boolean updateCategory = categoryDAO.updateCategory(
                    newCategoryId,
                    testCategoryName + " Updated",
                    "Danh muc da cap nhat"
            );
            System.out.println("Update category = " + updateCategory);
            System.out.println(categoryDAO.getCategoryById(newCategoryId));

            boolean deactivateCategory = categoryDAO.deactivateCategory(newCategoryId);
            System.out.println("Deactivate category = " + deactivateCategory);

            boolean deleteCategory = categoryDAO.deleteCategory(newCategoryId);
            System.out.println("Delete category = " + deleteCategory);
        }

        // =========================
        // TEST LOCATION DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF / MANAGER quản lý địa điểm du lịch dùng trong tour.
        // =========================
        LocationDAO locationDAO = new LocationDAO();

        System.out.println("\n===== TEST DANH SACH LOCATION =====");
        printList(locationDAO.getAllLocations());

        System.out.println("\n===== TEST SEARCH LOCATION =====");
        printList(locationDAO.searchLocations("Cần Thơ"));

        System.out.println("\n===== TEST THEM LOCATION MOI =====");
        String testLocationName = "Dia diem test " + System.currentTimeMillis();
        int newLocationId = locationDAO.createLocation(testLocationName, "Dia diem dung de test DAO");
        System.out.println("New LocationID = " + newLocationId);

        if (newLocationId > 0) {
            System.out.println(locationDAO.getLocationById(newLocationId));

            boolean updateLocation = locationDAO.updateLocation(
                    newLocationId,
                    testLocationName + " Updated",
                    "Dia diem da cap nhat"
            );
            System.out.println("Update location = " + updateLocation);
            System.out.println(locationDAO.getLocationById(newLocationId));

            boolean deleteLocation = locationDAO.deleteLocation(newLocationId);
            System.out.println("Delete location = " + deleteLocation);
        }

        // =========================
        // TEST TOUR LOCATION DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF / MANAGER quản lý các điểm tham quan theo ngày và thứ tự trong tour.
        // =========================
        TourLocationDAO tourLocationDAO = new TourLocationDAO();

        System.out.println("\n===== TEST TOUR LOCATIONS BY TOUR ID = 1 =====");
        printList(tourLocationDAO.getLocationsByTourId(TEST_TOUR_ID));

        System.out.println("\n===== TEST TOUR LOCATIONS TOUR ID = 1, DAY = 1 =====");
        printList(tourLocationDAO.getLocationsByTourIdAndDay(TEST_TOUR_ID, 1));

        System.out.println("\n===== TEST THEM TOUR LOCATION MOI =====");
        int testLocationId = findExistingLocationId(locationDAO);

        if (testLocationId <= 0) {
            System.out.println("Khong co LocationID hop le de test TourLocation.");
        } else {
            int newTourLocationId = tourLocationDAO.createTourLocation(
                    TEST_TOUR_ID,
                    testLocationId,
                    30,
                    99,
                    "Diem tham quan test DAO"
            );

            System.out.println("New TourLocationID = " + newTourLocationId);

            if (newTourLocationId > 0) {
                System.out.println(tourLocationDAO.getTourLocationById(newTourLocationId));

                boolean updateTourLocation = tourLocationDAO.updateTourLocation(
                        newTourLocationId,
                        testLocationId,
                        30,
                        98,
                        "Diem tham quan test DAO da cap nhat"
                );
                System.out.println("Update tour location = " + updateTourLocation);
                System.out.println(tourLocationDAO.getTourLocationById(newTourLocationId));

                int totalLocations = tourLocationDAO.countLocationsByTourId(TEST_TOUR_ID);
                System.out.println("Total locations by tour = " + totalLocations);

                boolean deleteTourLocation = tourLocationDAO.deleteTourLocation(newTourLocationId);
                System.out.println("Delete tour location = " + deleteTourLocation);
            }
        }

        // =========================
        // TEST FEATURED TOUR DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF / MANAGER dùng để quản lý tour nổi bật trên hệ thống.
        // =========================
        FeaturedTourDAO featuredTourDAO = new FeaturedTourDAO();

        System.out.println("\n===== TEST DANH SACH FEATURED TOUR ACTIVE =====");
        printList(featuredTourDAO.getActiveFeaturedTours());

        System.out.println("\n===== TEST DANH SACH FEATURED TOUR =====");
        printList(featuredTourDAO.getAllFeaturedTours());

        // =========================
        // TEST TOUR PRICE DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF / MANAGER quản lý lịch sử giá tour theo ngày hiệu lực.
        // =========================
        TourPriceDAO tourPriceDAO = new TourPriceDAO();

        System.out.println("\n===== TEST DANH SACH TOUR PRICE TOUR ID = 1 =====");
        printList(tourPriceDAO.getPricesByTourId(TEST_TOUR_ID));

        System.out.println("\n===== TEST CURRENT TOUR PRICE TOUR ID = 1 =====");
        TourPrice currentTourPrice = tourPriceDAO.getCurrentPriceByTourId(TEST_TOUR_ID);
        System.out.println(currentTourPrice);

        System.out.println("\n===== TEST TOUR PRICE THEO KHOANG NGAY =====");
        printList(tourPriceDAO.getPricesByEffectiveDateRange(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        ));

        System.out.println("\n===== TEST THEM TOUR PRICE MOI =====");
        LocalDate newTourPriceDate = LocalDate.now().plusYears(2).plusDays(1);

        int newTourPriceId = tourPriceDAO.createTourPrice(
                TEST_TOUR_ID,
                newTourPriceDate,
                new BigDecimal("1890000"),
                "Test them gia tour moi"
        );

        System.out.println("New PriceID = " + newTourPriceId);

        if (newTourPriceId > 0) {
            System.out.println(tourPriceDAO.getTourPriceById(newTourPriceId));

            boolean updatedTourPrice = tourPriceDAO.updateTourPrice(
                    newTourPriceId,
                    newTourPriceDate,
                    new BigDecimal("1990000"),
                    "Test cap nhat gia tour"
            );
            System.out.println("Update tour price = " + updatedTourPrice);
            System.out.println(tourPriceDAO.getTourPriceById(newTourPriceId));

            boolean deletedTourPrice = tourPriceDAO.deleteTourPrice(newTourPriceId);
            System.out.println("Delete tour price = " + deletedTourPrice);
        }

        // =========================
        // TEST DYNAMIC PRICE RULE DAO
        // Chức năng này dành cho STAFF / MANAGER
        // STAFF / MANAGER quản lý quy tắc giá động.
        // =========================
        DynamicPriceRuleDAO dynamicPriceRuleDAO = new DynamicPriceRuleDAO();

        System.out.println("\n===== TEST DANH SACH DYNAMIC PRICE RULE =====");
        printList(dynamicPriceRuleDAO.getAllRules());

        System.out.println("\n===== TEST ACTIVE RULES THEO NGAY 2026-07-10 =====");
        printList(dynamicPriceRuleDAO.getActiveRulesByDate(LocalDate.of(2026, 7, 10)));

        System.out.println("\n===== TEST RULES CONDITION TYPE = WEEKEND =====");
        printList(dynamicPriceRuleDAO.getRulesByConditionType("WEEKEND"));

        BigDecimal calculatedPrice =
                dynamicPriceRuleDAO.calculateFinalPrice(new BigDecimal("1000000"), LocalDate.of(2026, 7, 10));
        System.out.println("Calculated dynamic final price = " + calculatedPrice);

        System.out.println("\n===== TEST THEM DYNAMIC PRICE RULE MOI =====");
        int newRuleId = dynamicPriceRuleDAO.createRule(
                "Rule test DAO " + System.currentTimeMillis(),
                "HOLIDAY",
                new BigDecimal("12.50"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                5
        );

        System.out.println("New RuleID = " + newRuleId);

        if (newRuleId > 0) {
            System.out.println(dynamicPriceRuleDAO.getRuleById(newRuleId));

            boolean updateRule = dynamicPriceRuleDAO.updateRule(
                    newRuleId,
                    "Rule test DAO updated " + System.currentTimeMillis(),
                    "LOW_STOCK",
                    new BigDecimal("18.00"),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    6
            );
            System.out.println("Update rule = " + updateRule);
            System.out.println(dynamicPriceRuleDAO.getRuleById(newRuleId));

            boolean deactivateRule = dynamicPriceRuleDAO.deactivateRule(newRuleId);
            System.out.println("Deactivate rule = " + deactivateRule);

            boolean deleteRule = dynamicPriceRuleDAO.deleteRule(newRuleId);
            System.out.println("Delete rule = " + deleteRule);
        }

        testAuthService();

        System.out.println("\n===== TEST MAIN HOAN TAT =====");
    }

    private static int findAvailableScheduleId(List<TourSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return -1;
        }

        for (TourSchedule schedule : schedules) {
            if (schedule == null) {
                continue;
            }

            int remainingSlots = schedule.getAvailableSlots() - schedule.getBookedSlots();

            if (remainingSlots > 0) {
                return schedule.getScheduleId();
            }
        }

        return -1;
    }

    private static int findExistingLocationId(LocationDAO locationDAO) {
        if (locationDAO == null) {
            return -1;
        }

        List<Location> locations = locationDAO.getAllLocations();

        if (locations == null || locations.isEmpty()) {
            return -1;
        }

        for (Location location : locations) {
            if (location != null && location.getLocationId() > 0) {
                return location.getLocationId();
            }
        }

        return -1;
    }

    private static int findFirstActiveAddOnId(AddOnDAO addOnDAO) {
        if (addOnDAO == null) {
            return -1;
        }

        List<AddOn> addOns = addOnDAO.getAllActiveAddOns();

        if (addOns == null || addOns.isEmpty()) {
            return -1;
        }

        for (AddOn addOn : addOns) {
            if (addOn != null && addOn.getAddOnId() > 0) {
                return addOn.getAddOnId();
            }
        }

        return -1;
    }

    private static void runBookingPaymentTransportTests(int bookingId,
                                                        int userId,
                                                        int scheduleId,
                                                        BookingDAO bookingDAO,
                                                        PaymentDAO paymentDAO,
                                                        ETicketDAO eTicketDAO,
                                                        TransportRequestDAO transportDAO) {
        System.out.println("\n===== BOOKING VUA TAO =====");
        Booking booking = bookingDAO.getBookingById(bookingId);
        System.out.println(booking);

        if (booking == null) {
            System.out.println("Khong lay duoc booking vua tao.");
            return;
        }

        AddOnDAO addOnDAO = new AddOnDAO();
        BookingAddOnDAO bookingAddOnDAO = new BookingAddOnDAO();
        CouponDAO couponDAO = new CouponDAO();
        BookingPassengerDAO bookingPassengerDAO = new BookingPassengerDAO();

        // =========================
        // TEST BOOKING PASSENGER DAO
        // Chức năng này dành cho CUSTOMER / STAFF
        // CUSTOMER nhập thông tin hành khách khi booking còn PENDING.
        // STAFF hỗ trợ kiểm tra/sửa danh sách hành khách trước khi thanh toán.
        // =========================
        System.out.println("\n===== TEST DANH SACH HANH KHACH CUA BOOKING =====");
        printList(bookingPassengerDAO.getPassengersByBookingId(bookingId));

        System.out.println("\n===== TEST THEM HANH KHACH BABY KHONG CHIEM GHE =====");
        int babyPassengerId = bookingPassengerDAO.addPassenger(
                bookingId,
                "Em be test DAO",
                "BABY",
                BigDecimal.ZERO
        );

        System.out.println("New Baby PassengerID = " + babyPassengerId);

        if (babyPassengerId > 0) {
            System.out.println(bookingPassengerDAO.getPassengerById(babyPassengerId));

            boolean updateBaby = bookingPassengerDAO.updatePassenger(
                    babyPassengerId,
                    "Em be test DAO updated",
                    "BABY",
                    BigDecimal.ZERO
            );
            System.out.println("Update baby passenger = " + updateBaby);
            System.out.println(bookingPassengerDAO.getPassengerById(babyPassengerId));

            boolean deleteBaby = bookingPassengerDAO.deletePassenger(babyPassengerId);
            System.out.println("Delete baby passenger = " + deleteBaby);
        }

        System.out.println("\n===== TEST TONG HANH KHACH / SLOT CUA BOOKING =====");
        int passengerCount = bookingPassengerDAO.countPassengersByBookingId(bookingId);
        int occupiedSlots = bookingPassengerDAO.countOccupiedSlotsByBookingId(bookingId);
        BigDecimal totalPassengerPrice =
                bookingPassengerDAO.getTotalPassengerPriceByBookingId(bookingId);

        System.out.println("Passenger count = " + passengerCount);
        System.out.println("Occupied slots = " + occupiedSlots);
        System.out.println("Total passenger price = " + totalPassengerPrice);

        System.out.println("\n===== DANH SACH ADD-ON DANG HOAT DONG =====");
        printList(addOnDAO.getAllActiveAddOns());

        System.out.println("\n===== THEM ADD-ON VAO BOOKING =====");
        int addOnId = findFirstActiveAddOnId(addOnDAO);
        int bookingAddOnId = -1;

        if (addOnId <= 0) {
            System.out.println("Khong co add-on active de test.");
        } else {
            bookingAddOnId = bookingAddOnDAO.addBookingAddOn(bookingId, addOnId, 2);
            System.out.println("BookingAddOnID = " + bookingAddOnId);
        }

        System.out.println("\n===== DANH SACH ADD-ON CUA BOOKING =====");
        List<BookingAddOn> bookingAddOns = bookingAddOnDAO.getAddOnsByBookingId(bookingId);
        printList(bookingAddOns);

        if (bookingAddOnId > 0) {
            boolean updateAddOnQuantity = bookingAddOnDAO.updateQuantity(bookingAddOnId, 1);
            System.out.println("Update add-on quantity = " + updateAddOnQuantity);
        }

        System.out.println("\n===== TONG TIEN ADD-ON =====");
        BigDecimal totalAddOnAmount = bookingAddOnDAO.getTotalAddOnAmountByBookingId(bookingId);
        System.out.println("Total add-on amount = " + totalAddOnAmount);

        System.out.println("\n===== BOOKING SAU KHI THEM ADD-ON =====");
        Booking bookingAfterAddOn = bookingDAO.getBookingById(bookingId);
        System.out.println(bookingAfterAddOn);

        if (bookingAfterAddOn == null) {
            System.out.println("Khong lay duoc booking sau khi them add-on.");
            return;
        }

        // =========================
        // TEST COUPON DAO
        // Chức năng này dành cho CUSTOMER / STAFF / MANAGER
        // CUSTOMER áp mã giảm giá cho booking PENDING.
        // STAFF / MANAGER tạo, bật/tắt và quản lý coupon.
        // =========================
        System.out.println("\n===== TEST DANH SACH COUPON ACTIVE =====");
        printList(couponDAO.getActiveCoupons());

        System.out.println("\n===== TEST THEM COUPON MOI =====");
        String testCouponCode = "TEST" + System.currentTimeMillis();

        int newCouponId = couponDAO.createCoupon(
                testCouponCode,
                "PERCENTAGE",
                new BigDecimal("10"),
                1,
                1,
                new BigDecimal("200000"),
                LocalDate.now().plusDays(30),
                1
        );

        System.out.println("New CouponID = " + newCouponId);

        if (newCouponId > 0) {
            Coupon newCoupon = couponDAO.getCouponById(newCouponId);
            System.out.println(newCoupon);

            CouponDAO.CouponCheckResult checkResult =
                    couponDAO.checkCouponForUser(
                            testCouponCode,
                            userId,
                            bookingAfterAddOn.getTotalPrice()
                    );
            System.out.println(checkResult);

            boolean applyCoupon = couponDAO.applyCouponToPendingBooking(bookingId, testCouponCode);
            System.out.println("Apply coupon to booking = " + applyCoupon);

            Booking bookingAfterCoupon = bookingDAO.getBookingById(bookingId);
            System.out.println("\n===== BOOKING SAU KHI AP COUPON =====");
            System.out.println(bookingAfterCoupon);

            boolean applyCouponAgain = couponDAO.applyCouponToPendingBooking(bookingId, testCouponCode);
            System.out.println("Apply coupon lan 2, ky vong false = " + applyCouponAgain);

            if (bookingAfterCoupon != null) {
                bookingAfterAddOn = bookingAfterCoupon;
            }
        }

        BigDecimal finalPrice = bookingAfterAddOn.getFinalPrice();

        if (finalPrice == null || finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("FinalPrice khong hop le, dung test payment.");
            return;
        }

        System.out.println("\n===== TEST THANH TOAN BOOKING =====");
        int paymentId = paymentDAO.payBooking(
                bookingId,
                finalPrice,
                "BANKING",
                "TXN_TEST_" + System.currentTimeMillis()
        );
        System.out.println("PaymentID = " + paymentId);

        if (paymentId <= 0) {
            System.out.println("Thanh toan that bai, khong test ticket.");
            return;
        }

        System.out.println("\n===== PAYMENT VUA TAO =====");
        Payment payment = paymentDAO.getPaymentById(paymentId);
        System.out.println(payment);

        System.out.println("\n===== BOOKING SAU THANH TOAN =====");
        Booking bookingAfterPayment = bookingDAO.getBookingById(bookingId);
        System.out.println(bookingAfterPayment);

        System.out.println("\n===== E-TICKET CUA BOOKING =====");
        ETicket ticket = eTicketDAO.getTicketByBookingId(bookingId);
        System.out.println(ticket);

        if (ticket != null) {
            System.out.println("\n===== KIEM TRA TICKET VALID =====");
            boolean valid = eTicketDAO.isTicketValid(ticket.getTicketCode());
            System.out.println("Ticket valid? " + valid);

            System.out.println("\n===== TIM TICKET THEO CODE =====");
            ETicket ticketByCode = eTicketDAO.getTicketByCode(ticket.getTicketCode());
            System.out.println(ticketByCode);
        }

        System.out.println("\n===== DANH SACH VE CUA USER ID = " + userId + " =====");
        printList(eTicketDAO.getTicketsByUserId(userId));

        System.out.println("\n===== DANH SACH VE ACTIVE CUA USER ID = " + userId + " =====");
        printList(eTicketDAO.getActiveTicketsByUserId(userId));

        System.out.println("\n===== TEST TAO YEU CAU VAN CHUYEN =====");
        int transportRequestId = transportDAO.createTransportRequest(
                scheduleId,
                bookingId,
                "Cong ty Xe Mien Tay",
                "0909999999",
                "Ben xe Can Tho",
                "Miet vuon Can Tho",
                1,
                "Can xe 16 cho, don khach dung gio",
                2
        );
        System.out.println("TransportRequestID = " + transportRequestId);

        if (transportRequestId <= 0) {
            System.out.println("Tao yeu cau van chuyen that bai.");
            return;
        }

        System.out.println("\n===== XEM YEU CAU VAN CHUYEN VUA TAO =====");
        TransportRequest transportRequest = transportDAO.getTransportRequestById(transportRequestId);
        System.out.println(transportRequest);

        System.out.println("\n===== CAP NHAT DOI TAC VAN CHUYEN =====");
        boolean updatePartner = transportDAO.updatePartnerInfo(
                transportRequestId,
                "Cong ty Xe Du Lich ABC",
                "0911111111",
                "Da gui thong tin cho doi tac ABC"
        );
        System.out.println("Update partner = " + updatePartner);

        System.out.println("\n===== CAP NHAT DIEM DON TRA =====");
        boolean updatePickupDropoff = transportDAO.updatePickupDropoff(
                transportRequestId,
                "Ben xe Can Tho - Cong chinh",
                "Khu du lich Miet vuon Can Tho"
        );
        System.out.println("Update pickup/dropoff = " + updatePickupDropoff);

        System.out.println("\n===== CAP NHAT STATUS = SENT =====");
        boolean sent = transportDAO.updateTransportRequestStatus(transportRequestId, "SENT");
        System.out.println("Update SENT = " + sent);

        System.out.println("\n===== CAP NHAT STATUS = CONFIRMED =====");
        boolean confirmed = transportDAO.updateTransportRequestStatus(transportRequestId, "CONFIRMED");
        System.out.println("Update CONFIRMED = " + confirmed);

        System.out.println("\n===== YEU CAU VAN CHUYEN SAU CAP NHAT =====");
        System.out.println(transportDAO.getTransportRequestById(transportRequestId));

        System.out.println("\n===== DANH SACH YEU CAU VAN CHUYEN THEO BOOKING =====");
        printList(transportDAO.getTransportRequestsByBookingId(bookingId));

        System.out.println("\n===== DANH SACH YEU CAU VAN CHUYEN CONFIRMED =====");
        printList(transportDAO.getTransportRequestsByStatus("CONFIRMED"));
    }

    private static void testAuthService() {
        AuthService authService = new AuthService();

        System.out.println("\n===== TEST AUTH SERVICE =====");

        System.out.println("\n===== DEBUG STAFF LOGIN =====");
        System.out.println(authService.debugLogin("staff@gmail.com", "123"));

        System.out.println("\n===== DEBUG MANAGER LOGIN =====");
        System.out.println(authService.debugLogin("admin@gmail.com", "123"));

        System.out.println("\n===== DEBUG CUSTOMER LOGIN =====");
        System.out.println(authService.debugLogin("0912345678", "123456"));
    }

    private static void printList(List<?> items) {
        if (items == null || items.isEmpty()) {
            System.out.println("Khong co du lieu.");
            return;
        }

        for (Object item : items) {
            System.out.println(item);
        }
        AuthService authService = new AuthService();
    
            System.out.println(authService.debugLogin("staff@gmail.com", "123"));
            System.out.println(authService.debugLogin("admin@gmail.com", "123"));
            System.out.println(authService.debugLogin("0903333333", "123456")); 
    }

}
