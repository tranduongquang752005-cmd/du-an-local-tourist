import model.User;
import service.AuthService;
import service.CustomerService;
import service.QrService;
import service.StaffService;

public class TestQrMain {

    public static void main(String[] args) {
        System.out.println("\n========== QR TEST START ==========");

        AuthService authService = new AuthService();
        CustomerService customerService = new CustomerService();
        StaffService staffService = new StaffService();
        QrService qrService = new QrService();

        User customer = login(authService, "0912345678", "123456");
        User staff = login(authService, "staff@gmail.com", "123");

        if (customer == null || staff == null) {
            System.out.println("Skip QR test: customer hoac staff login failed.");
            return;
        }

        System.out.println("\n===== CREATE NEW PENDING BOOKING FOR PAYMENT QR =====");
        CustomerService.ServiceResult<Integer> createBookingResult =
                customerService.createBooking(customer, 1, 1, 1, 0, 0, null);

        System.out.println(createBookingResult);

        if (!createBookingResult.isSuccess() || createBookingResult.getData() == null) {
            System.out.println("Skip QR test: create booking failed.");
            return;
        }

        int bookingId = createBookingResult.getData();

        System.out.println("\n===== PAYMENT QR SIMULATION =====");
        QrService.ServiceResult<QrService.QrInfo> paymentQrResult =
                qrService.createPaymentQr(
                        staff,
                        bookingId,
                        "VCB",
                        "1234567890",
                        "CONG TY DU LICH TEST"
                );

        System.out.println(paymentQrResult);

        if (paymentQrResult.isSuccess() && paymentQrResult.getData() != null) {
            System.out.println("Payment QR DataUri length = "
                    + paymentQrResult.getData().getQrImageDataUri().length());
            System.out.println("Payment QR amount = " + paymentQrResult.getData().getAmount());
        }

        System.out.println("\n===== CONFIRM PAID TO CREATE E-TICKET =====");
        StaffService.ServiceResult<Integer> paymentResult =
                staffService.confirmBookingPaidByBanking(
                        staff,
                        bookingId,
                        "TXN_QR_TEST_" + System.currentTimeMillis()
                );

        System.out.println(paymentResult);

        System.out.println("\n===== CUSTOMER TICKET QR =====");
        QrService.ServiceResult<QrService.QrInfo> customerTicketQrResult =
                qrService.createMyTicketQr(customer, bookingId);

        System.out.println(customerTicketQrResult);

        if (customerTicketQrResult.isSuccess() && customerTicketQrResult.getData() != null) {
            System.out.println("Ticket QR DataUri length = "
                    + customerTicketQrResult.getData().getQrImageDataUri().length());
            System.out.println("Ticket code = " + customerTicketQrResult.getData().getTicketCode());
        }

        System.out.println("\n===== STAFF TICKET QR =====");
        System.out.println(qrService.createTicketQrForOperation(staff, bookingId));

        System.out.println("\n========== QR TEST END ==========");
    }

    private static User login(AuthService authService, String identifier, String password) {
        AuthService.AuthResult result = authService.login(identifier, password);
        System.out.println("Login " + identifier + " = " + result);

        if (!result.isSuccess()) {
            return null;
        }

        return result.getUser();
    }
}
