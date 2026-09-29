# NCSA SQL Injection Demo

A beginner-friendly Java console application that demonstrates how SQL injection
attacks work and how to prevent them using JDBC PreparedStatement.

Built as a practical assignment for the National Cyber Security Authority (NCSA)
internship recruitment process.

---

## Project Structure

```
NCSA-SQL-Injection/
├── pom.xml
├── .gitignore
└── src/main/
    ├── resources/
    │   └── db.properties          <- database connection settings (not committed)
    └── java/com/ncsa/sqldemo/
        ├── Main.java              <- entry point, runs all test cases
        ├── DatabaseSetup.java     <- creates the users table and dummy accounts
        ├── VulnerableLogin.java   <- unsafe login using string concatenation
        └── SecureLogin.java       <- safe login using PreparedStatement
```

---

## Prerequisites

- Java 17
- Apache Maven 3.x
- PostgreSQL (installed and running locally)
- VS Code or any terminal

---

## Database Setup

1. Open SQL Shell (psql) from the Windows Start menu
2. Press Enter to accept defaults for Server, Database, Port, and Username
3. Enter your PostgreSQL password when prompted
4. Run this command to create the demo database:

```sql
CREATE DATABASE ncsa_demo;
```

5. Type `\q` to exit

The application will automatically create the `users` table and insert
dummy accounts every time it runs. You do not need to create the table manually.

---

## Configuration

Open `src/main/resources/db.properties` and set your PostgreSQL connection details:

```
db.url=jdbc:postgresql://localhost:5432/ncsa_demo
db.user=postgres
db.password=<your_local_postgresql_password>
```

This file is listed in `.gitignore` and will never be committed to version control.
Credentials are loaded at runtime from this file — they are not hardcoded in any
Java source file.

---

## How to Run

Open a terminal in the project folder and run:

```
mvn clean compile exec:java
```

Expected output: the program runs 4 test cases against both the vulnerable
and secure login and prints the results side by side.

---

## Sample Data

The application creates these dummy accounts automatically on every run:

| Username | Role  |
|----------|-------|
| alice    | user  |
| bob      | user  |
| admin    | admin |

The passwords used are simple demo values chosen for readability only.
They are not real credentials and exist only in a local test database.

---

## How to Safely Demonstrate the SQL Injection

This demo runs entirely on your local machine against a local test database
with dummy accounts. No real users, real credentials, or external systems
are involved.

The two attack inputs used in the demo are:

**Attack 1 — SQL Comment injection**

The attacker enters a crafted username that ends with `'--`.
The `--` turns everything after it into a SQL comment, removing the password check.

**Attack 2 — OR 1=1 injection**

The attacker enters a crafted username containing `OR '1'='1'--`.
This adds a condition that is always true, bypassing the login entirely.

Run the program and observe the `[SQL SENT]` output lines to see exactly
how the query changes when attack input is used.

---

## Why the Vulnerable Version Can Be Exploited

The vulnerable login in `VulnerableLogin.java` builds its SQL query like this:

```java
String query = "SELECT * FROM users WHERE username = '" + username +
               "' AND password = '" + password + "'";
```

The user input is pasted directly into the query string. The database
receives one combined string and cannot tell which part is SQL code written
by the developer and which part came from the user.

**Example — SQL Comment attack:**

When the attacker enters a crafted username ending with `'--`, the final query becomes:

```
SELECT * FROM users WHERE username = 'admin'--' AND password = '...'
```

The `--` is a SQL comment. Everything after it is ignored, including the
entire password check. The database only checks the username, so the login
succeeds without a password.

**Example — OR 1=1 attack:**

When the attacker enters a crafted username containing `OR '1'='1'--`, the query becomes:

```
SELECT * FROM users WHERE username = '' OR '1'='1'--' AND password = '...'
```

The condition `'1'='1'` is always true. The `--` removes the password check.
The WHERE clause is always true, so the database returns the first user in
the table and the login succeeds without any valid credentials.

---

## Why the Secure Version Blocks It

The secure login in `SecureLogin.java` uses PreparedStatement:

```java
String query = "SELECT * FROM users WHERE username = ? AND password = ?";
PreparedStatement pstmt = conn.prepareStatement(query);
pstmt.setString(1, username);
pstmt.setString(2, password);
```

The SQL structure is sent to the database first as a fixed template with `?`
placeholders. The database compiles this template before any user input is
involved. The user input is then passed separately as plain data values.

The database driver automatically escapes any special characters in the input.
So when the attacker enters a crafted username, the database searches for a user
whose username is literally that exact string — not SQL code. No such user
exists, so the login fails.

**The key principle: with PreparedStatement, user input can never change the
structure or logic of the SQL query.**

---

## Main Differences Between the Two Approaches

| Aspect | Vulnerable (Statement) | Secure (PreparedStatement) |
|---|---|---|
| Query building | String concatenation with user input | Fixed template with ? placeholders |
| User input role | Becomes part of SQL code | Treated as data only |
| SQL injection | Possible | Not possible |
| Special characters | Interpreted as SQL | Automatically escaped |
| Code readability | Simple but dangerous | Equally simple and safe |

---

## Additional Security Improvements

These are separate from SQL injection prevention but important to mention:

- **Password hashing**: Passwords in this demo are plain text for readability.
  Real applications must hash passwords using BCrypt or Argon2 before storing them.

- **Input validation**: Reject input that contains unexpected characters before
  it reaches the database. This is a defence-in-depth measure, not a substitute
  for PreparedStatement.

- **Least privilege**: The database user the application connects with should
  only have SELECT and INSERT permissions, not DROP or admin rights.

- **Safe error handling**: Never show raw database error messages to users.
  Log errors internally and show a generic message to the user.

- **Credentials management**: Connection details are stored in `db.properties`
  and excluded from version control via `.gitignore`. In production, use
  environment variables or a secrets manager such as AWS Secrets Manager.

---

## Technologies Used

- Java 17
- PostgreSQL
- JDBC (PostgreSQL driver 42.7.12)
- Apache Maven 3.x
