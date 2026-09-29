package com.ncsa.sqldemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SecureLogin demonstrates a SAFE login function using PreparedStatement.
 *
 * The fix: the SQL structure is defined first with placeholders (?).
 * User input is then passed separately as data — never as SQL code.
 * The database driver handles escaping, so injection is impossible.
 */
public class SecureLogin {

    public static String login(String username, String password) {
        // The query structure is fixed. The ? marks are placeholders for user input.
        // No matter what the user types, it will only ever be treated as a value,
        // never as SQL code.
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        System.out.println("  [SQL SENT] " + query);
        // Sanitize before printing to prevent log injection
        System.out.println("  [PARAM 1 - username] " + sanitize(username));
        // Password is masked — never print real password values to output
        System.out.println("  [PARAM 2 - password] ********");

        try (Connection conn = DriverManager.getConnection(
                DatabaseSetup.DB_URL, DatabaseSetup.DB_USER, DatabaseSetup.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set the first ? to the username value
            pstmt.setString(1, username);
            // Set the second ? to the password value
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return "LOGIN SUCCESS — Welcome, " + rs.getString("username") + "!";
                } else {
                    return "LOGIN FAILED — No matching account found.";
                }
            }

        } catch (SQLException e) {
            // Return a generic message — never expose raw database errors
            return "LOGIN ERROR — A database error occurred.";
        }
    }

    // Removes newline characters from a string before printing.
    // This prevents log injection, where an attacker could forge extra log lines.
    private static String sanitize(String input) {
        return input.replaceAll("[\r\n]", " ");
    }
}
