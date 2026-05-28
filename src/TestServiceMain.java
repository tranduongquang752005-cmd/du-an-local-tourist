import model.Booking;
import model.User;
import service.AuthService;
import service.CustomerService;
import service.ManagerService;
import service.StaffService;


public class TestServiceMain {

    public static void main(String[] args) {
        System.out.println("\n========== SERVICE LAYER TEST START ==========");

        AuthService authService = new AuthService();
        CustomerService customerService = new CustomerService();
        StaffService staffService = new StaffService();
        ManagerService managerService = new ManagerService();

        User customer = loginAndPrint(authService, "0912345678", "123456", "CUSTOMER");
        User staff = loginAndPrint(authService, "staff@gmail.com", "123", "STAFF");
        User manager = loginAndPrint(authService, "admin@gmail.com", "123", "MANAGER");

        if (customer == null) {
            System.out.println("\n[WARN] CUSTOMER test account login failed.");
            System.out.println("       Neu customer 0912345678 chua ton tai, hay chay Main test register customer truoc.");
        }

        if (staff == null || manager == null) {
            System.out.println("\n[WARN] STAFF/MANAGER login failed.");
            System.out.println("       Thuong do PasswordHash trong SQL seed van la REPLICATE('x',60).");
            System.out.println("       Can update hash that cua password 123 cho staff@gmail.com/admin@gmail.com.");
        }

        testPublicCustomerFunctions(customerService, customer);
        testRolePermissions(customerService, staffService, managerService, customer, staff, manager);
        testBookingAndPaymentFlow(customerService, staffService, customer, staff, manager);
        testManagerFunctions(managerService, staff, manager);

        System.out.println("\n========== SERVICE LAYER TEST END ==========");
    }

    private static User loginAndPrint(AuthService authService,
                                      String identifier,
                                      String password,
                                      String label) {
        System.out.println("\n===== LOGIN " + label + " =====");
        System.out.println("Debug = " + authService.debugLogin(identifier, password));

        AuthService.AuthResult result = authService.login(identifier, password);
        System.out.println("Result = " + result);

        if (!result.isSuccess()) {
            return null;
        }

        return result.getUser();
    }

    private static void testPublicCustomerFunctions(CustomerService customerService, User customer) {
        System.out.println("\n===== TEST PUBLIC / CUSTOMER TOUR FUNCTIONS =====");

        System.out.println("Active tours:");
        System.out.println(customerService.getActiveTours(customer));

        System.out.println("Tours paging:");
        System.out.println(customerService.getToursPaging(customer, 1, 3));

        System.out.println("Search tours:");
        System.out.println(customerService.searchTours(customer, "Cần Thơ"));

        System.out.println("Featured tours:");
        System.out.println(customerService.getFeaturedTours(customer));

        System.out.println("Popular tours:");
        System.out.println(customerService.getPopularTours(customer, 6));

        System.out.println("Tour detail:");
        System.out.println(customerService.getTourDetail(customer, 1));

        System.out.println("Tour images:");
        System.out.println(customerService.getTourImages(customer, 1));

        System.out.println("Available schedules:");
        System.out.println(customerService.getAvailableSchedules(customer, 1));

        System.out.println("Tour reviews:");
        System.out.println(customerService.getTourReviews(customer, 1));

        System.out.println("Average rating:");
        System.out.println(customerService.getAverageRating(customer, 1));
    }

    private static void testRolePermissions(CustomerService customerService,
                                            StaffService staffService,
                                            ManagerService managerService,
                                            User customer,
                                            User staff,
                                            User manager) {
        System.out.println("\n===== TEST ROLE PERMISSIONS =====");

        System.out.println("\n[Expect FAIL] STAFF tries customer create booking:");
        if (staff != null) {
            System.out.println(customerService.createBooking(staff, 1, 1, 1, 0, 0, null));
        } else {
            System.out.println("Skip: staff null.");
        }

        System.out.println("\n[Expect FAIL] CUSTOMER tries staff booking detail:");
        if (customer != null) {
            System.out.println(staffService.getBookingDetail(customer, 1));
        } else {
            System.out.println("Skip: customer null.");
        }

        System.out.println("\n[Expect FAIL] STAFF tries manager revenue:");
        if (staff != null) {
            System.out.println(managerService.getMonthlyRevenue(staff, 2026));
        } else {
            System.out.println("Skip: staff null.");
        }

        System.out.println("\n[Expect SUCCESS] MANAGER revenue:");
        if (manager != null) {
            System.out.println(managerService.getMonthlyRevenue(manager, 2026));
        } else {
            System.out.println("Skip: manager null.");
        }
    }

    private static void testBookingAndPaymentFlow(CustomerService customerService,
                                                  StaffService staffService,
                                                  User customer,
                                                  User staff,
                                                  User manager) {
        System.out.println("\n===== TEST BOOKING + QR + PAYMENT FLOW =====");

        if (customer == null) {
            System.out.println("Skip booking flow: customer null.");
            return;
        }

        User operator = staff != null ? staff : manager;

        if (operator == null) {
            System.out.println("Skip payment/QR flow: staff and manager are null.");
            return;
        }

        int tourId = 1;
        int scheduleId = 1;

        System.out.println("\nCreate booking:");
        CustomerService.ServiceResult<Integer> createBookingResult =
                customerService.createBooking(customer, tourId, scheduleId, 1, 0, 0, null);

        System.out.println(createBookingResult);

        if (!createBookingResult.isSuccess() || createBookingResult.getData() == null) {
            System.out.println("Skip next booking tests because create booking failed.");
            return;
        }

        int bookingId = createBookingResult.getData();

        System.out.println("\nGet my booking:");
        CustomerService.ServiceResult<Booking> bookingResult =
                customerService.getMyBookingDetail(customer, bookingId);

        System.out.println(bookingResult);

        if (!bookingResult.isSuccess() || bookingResult.getData() == null) {
            System.out.println("Skip QR/payment tests because booking detail failed.");
            return;
        }

        Booking booking = bookingResult.getData();
        System.out.println("FinalPrice expected for QR/payment = " + booking.getFinalPrice());

        System.out.println("\nGenerate QR by staff/manager:");
        StaffService.ServiceResult<StaffService.PaymentQrInfo> qrResult =
                staffService.generateBankTransferQr(
                        operator,
                        bookingId,
                        "VCB",
                        "1234567890",
                        "CONG TY DU LICH TEST"
                );

        System.out.println(qrResult);

        if (qrResult.isSuccess() && qrResult.getData() != null) {
            System.out.println("QR amount = " + qrResult.getData().getAmount());
            System.out.println("QR URL = " + qrResult.getData().getQrImageUrl());

            if (booking.getFinalPrice() != null
                    && qrResult.getData().getAmount().compareTo(booking.getFinalPrice()) == 0) {
                System.out.println("[OK] QR amount matches Booking.FinalPrice.");
            } else {
                System.out.println("[WARN] QR amount does NOT match Booking.FinalPrice.");
            }
        }

        System.out.println("\nConfirm paid by banking:");
        StaffService.ServiceResult<Integer> paymentResult =
                staffService.confirmBookingPaidByBanking(
                        operator,
                        bookingId,
                        "TXN_TEST_" + System.currentTimeMillis()
                );

        System.out.println(paymentResult);

        System.out.println("\nBooking after payment:");
        System.out.println(staffService.getBookingDetail(operator, bookingId));

        System.out.println("\nCustomer ticket after payment:");
        System.out.println(customerService.getMyTicketByBooking(customer, bookingId));
    }

    private static void testManagerFunctions(ManagerService managerService,
                                             User staff,
                                             User manager) {
        System.out.println("\n===== TEST MANAGER FUNCTIONS =====");

        System.out.println("\n[Expect FAIL] Staff get audit:");
        if (staff != null) {
            System.out.println(managerService.getRecentAuditLogs(staff, 5));
        } else {
            System.out.println("Skip: staff null.");
        }

        System.out.println("\n[Expect SUCCESS] Manager get audit:");
        if (manager != null) {
            System.out.println(managerService.getRecentAuditLogs(manager, 5));
            System.out.println(managerService.getRevenueSummary(
                    manager,
                    java.time.LocalDate.of(2026, 1, 1),
                    java.time.LocalDate.of(2026, 12, 31)
            ));
            System.out.println(managerService.getActiveConfigs(manager));
            System.out.println(managerService.getActiveCoupons(manager));
        } else {
            System.out.println("Skip: manager null.");
        }
    }
}
