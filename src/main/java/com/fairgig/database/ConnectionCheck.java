package com.fairgig.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Small tool for the team: run its main() to see whether your .env settings work
 * and how many tables and users the database has. Prints no password.
 */
public final class ConnectionCheck {

    private ConnectionCheck() {}

    public static void main(String[] args) {
        System.out.println("Connecting to " + DatabaseConnection.url() + " ...");
        try (Connection c = DatabaseConnection.getConnection(); Statement st = c.createStatement()) {
            System.out.println("Connection OK.");
            try (ResultSet rs = st.executeQuery(
                    "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")) {
                rs.next();
                System.out.println("Tables found: " + rs.getInt(1) + " (expected 24)");
            }
            try (ResultSet rs = st.executeQuery("SELECT count(*) FROM users")) {
                rs.next();
                System.out.println("Users found: " + rs.getInt(1));
            } catch (SQLException e) {
                System.out.println("The 'users' table is missing. Run schema.sql first.");
            }
        } catch (SQLException e) {
            System.out.println("Connection FAILED: " + e.getMessage());
            System.out.println("Check DB_HOST, DB_PORT, DB_NAME, DB_USER and DB_PASSWORD in your .env file.");
        }
    }
}
