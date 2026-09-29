package com.ncsa.sqldemo;

/**
 * Main is the entry point of the SQL Injection Demo.
 *
 * It runs the database setup, then tests both the vulnerable and secure
 * login functions using the same set of test cases so results can be compared.
 */
public class Main {

    public static void main(String[] args) {

        // Step 1: Set up the database with a fresh users table and dummy accounts
        DatabaseSetup.setup();

        System.out.println();
        System.out.println("=================================================================");
        System.out.println("         NCSA SQL INJECTION DEMONSTRATION");
        System.out.println("=================================================================");

        // --- TEST CASES ---
        // Each test case is: { username input, password input, description }
        String[][] testCases = {
            // Normal correct login
            { "alice",  "alice123",          "Normal login with correct credentials" },
            // Normal wrong password
            { "alice",  "wrongpassword",     "Normal login with wrong password" },
            // SQL injection: bypass password check entirely
            // The ' OR '1'='1'-- closes the username field, adds a always-true condition,
            // then comments out the rest so the password check is never evaluated.
            { "' OR '1'='1'--", "anything",  "ATTACK: Bypass login with OR 1=1 injection" },
            // SQL injection: comment out the password check
            // The -- is a SQL comment. Everything after it is ignored,
            // including the AND password = '...' part.
            { "admin'--", "anything",        "ATTACK: Bypass password check using SQL comment" },
        };

        // ---------------------------------------------------------------
        // PART 1: Run all test cases through the VULNERABLE login
        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("-----------------------------------------------------------------");
        System.out.println("  PART 1 — VULNERABLE LOGIN (unsafe string concatenation)");
        System.out.println("-----------------------------------------------------------------");

        for (String[] tc : testCases) {
            System.out.println();
            System.out.println("  TEST: " + tc[2]);
            System.out.println("  Input username : " + tc[0]);
            System.out.println("  Input password : " + tc[1]);
            String result = VulnerableLogin.login(tc[0], tc[1]);
            System.out.println("  RESULT: " + result);
        }

        // ---------------------------------------------------------------
        // PART 2: Run the same test cases through the SECURE login
        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("-----------------------------------------------------------------");
        System.out.println("  PART 2 — SECURE LOGIN (PreparedStatement)");
        System.out.println("-----------------------------------------------------------------");

        for (String[] tc : testCases) {
            System.out.println();
            System.out.println("  TEST: " + tc[2]);
            System.out.println("  Input username : " + tc[0]);
            System.out.println("  Input password : " + tc[1]);
            String result = SecureLogin.login(tc[0], tc[1]);
            System.out.println("  RESULT: " + result);
        }

        System.out.println();
        System.out.println("=================================================================");
        System.out.println("  DEMO COMPLETE");
        System.out.println("=================================================================");
        System.out.println();
        System.out.println("KEY TAKEAWAY:");
        System.out.println("  The attack inputs that bypassed the VULNERABLE login");
        System.out.println("  were blocked by the SECURE login because PreparedStatement");
        System.out.println("  treats all user input as plain data, never as SQL code.");
    }
}
