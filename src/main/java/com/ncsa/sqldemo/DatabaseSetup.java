package com.ncsa.sqldemo;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * DatabaseSetup connects to PostgreSQL and prepares the demo environment.
 * It creates the users table and inserts dummy accounts for testing.
 * Credentials are loaded from db.properties, not hardcoded in source code.
 */
public class DatabaseSetup {

    static final String DB_URL;
    static final String DB_USER;
    static final String DB_PASSWORD;

    // Load connection details from db.properties when the class is first used
    static {
        Properties props = new Properties();
        try (InputStream input = DatabaseSetup.class
                .getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException(
                    "db.properties not found. Please create it in src/main/resources/");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties: " + e.getMessage(), e);
        }
        DB_URL      = props.getProperty("db.url");
        DB_USER     = props.getProperty("db.user");
        DB_PASSWORD = props.getProperty("db.password");
    }

    public static void setup() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Drop the table if it already exists so we start fresh every run
            stmt.execute("DROP TABLE IF EXISTS users");

            // Create a simple users table with id, username, and password columns
            stmt.execute(
                "CREATE TABLE users (" +
                "  id       SERIAL PRIMARY KEY," +
                "  username VARCHAR(50)  NOT NULL," +
                "  password VARCHAR(100) NOT NULL" +
                ")"
            );

            // Insert 3 dummy accounts for testing.
            // NOTE: Passwords are plain text intentionally so the demo output
            // is easy to read. Real applications must hash passwords with BCrypt.
            stmt.execute("INSERT INTO users (username, password) VALUES ('alice', 'alice123')");
            stmt.execute("INSERT INTO users (username, password) VALUES ('bob',   'bob456')");
            stmt.execute("INSERT INTO users (username, password) VALUES ('admin', 'adminpass')");

            System.out.println("[SETUP] Database ready. Table 'users' created with 3 dummy accounts.");

        } catch (SQLException e) {
            // Log the error type only — never expose raw DB error details to output
            System.out.println("[SETUP ERROR] Database error: " + e.getMessage());
        }
    }
}
