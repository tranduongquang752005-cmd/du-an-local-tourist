import dao.UserDAO;
import service.AuthService;

public class UpdateInternalPasswordMain {

    public static void main(String[] args) {
        System.out.println("\n===== UPDATE INTERNAL PASSWORDS =====");

        UserDAO userDAO = new UserDAO();

        boolean updateStaff = userDAO.updatePasswordByLoginName("staff@gmail.com", "123");
        boolean updateManager = userDAO.updatePasswordByLoginName("admin@gmail.com", "123");

        System.out.println("Update staff@gmail.com password = " + updateStaff);
        System.out.println("Update admin@gmail.com password = " + updateManager);

        AuthService authService = new AuthService();

        System.out.println("\n===== VERIFY STAFF LOGIN =====");
        System.out.println(authService.debugLogin("staff@gmail.com", "123"));
        System.out.println(authService.login("staff@gmail.com", "123"));

        System.out.println("\n===== VERIFY MANAGER LOGIN =====");
        System.out.println(authService.debugLogin("admin@gmail.com", "123"));
        System.out.println(authService.login("admin@gmail.com", "123"));

        System.out.println("\n===== DONE =====");
    }
}
