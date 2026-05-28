package config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlserver://localhost:1433;"
            + "databaseName=DuLich_DB;"
            + "encrypt=true;"
            + "trustServerCertificate=true;";

    private static final String USER = "java_user";
    private static final String PASSWORD = "123456";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("Ket noi database that bai!");
            e.printStackTrace();
            return null;
        }
    }
}