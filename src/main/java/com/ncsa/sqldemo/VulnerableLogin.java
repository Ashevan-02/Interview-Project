package com.ncsa.sqldemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * VulnerableLogin demonstrates an UNSAFE login function.
 *
 * The problem: user input is pasted directly into the SQL query string.
 * An attacker can type special SQL characters to change the query's logic
 * and bypass the password check entirely. This is SQL injection.
 */
public class VulnerableLogin {

    public static String login(String username, String password) {
        // The query is built by joining user input directly into the string.
        // If the user types SQL code as their input, it becomes part of the query.
        String query = "SELECT * FROM users WHERE username = '" + username +
                       "' AND password = '" + password + "'";

        // Print the final query so we can see exactly what gets sent to the database.
        // Newlines are stripped to prevent log injection — a real security practice.
        System.out.println("  [SQL SENT] " + sanitize(query));

        try (Connection conn = DriverManager.getConnection(
                DatabaseSetup.DB_URL, DatabaseSetup.DB_USER, DatabaseSetup.DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                // A row was returned, so the database accepted the login
                return "LOGIN SUCCESS — Welcome, " + rs.getString("username") + "!";
            } else {
                return "LOGIN FAILED — No matching account found.";
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
