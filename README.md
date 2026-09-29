# NCSA SQL Injection Demo

A Java console application that demonstrates how an SQL injection attack happens
and how it can be prevented using PreparedStatement.

---

## What this project does

This project simulates a login system connected to a real PostgreSQL database.
It runs the same test cases against two versions of the login:

- **VulnerableLogin** — built with unsafe string concatenation. Gets bypassed by attacks.
- **SecureLogin** — built with PreparedStatement. Blocks every attack.

The output shows exactly what SQL the database receives in each case so you can
see the attack happening in real time.

---

## What you need before you start

Make sure you have these installed on your machine:

- Java 17
- Apache Maven 3.x
- PostgreSQL (running locally)
- VS Code or any terminal

To check if Java and Maven are installed, open a terminal and run:

```
java -version
mvn -version
```

---

## Step 1 — Create the database

Open SQL Shell (psql) from your Start menu.

Press Enter to accept the defaults for Server, Database, Port, and Username.
Type your PostgreSQL password when prompted.

Then run this command:

```sql
CREATE DATABASE ncsa_demo;
```

You should see:
```
CREATE DATABASE
```

Type `\q` to exit.

---

## Step 2 — Configure your database credentials

Open this file:

```
src/main/resources/db.properties
```

It looks like this:

```
db.url=jdbc:postgresql://localhost:5432/ncsa_demo
db.user=postgres
db.password=your_password_here
```

Replace `your_password_here` with your actual PostgreSQL password and save the file.

This file is listed in `.gitignore` and will never be committed to GitHub.
Your password stays on your machine only.

---

## Step 3 — Run the project

Open a terminal inside the project folder and run:

```
mvn clean compile exec:java
```

Maven will compile the 4 Java files and run the program.

---

## Step 4 — What you will see

The program runs in 2 parts.

---

### Part 1 — Vulnerable Login (the careless developer)

```
-----------------------------------------------------------------
  PART 1 — VULNERABLE LOGIN (unsafe string concatenation)
-----------------------------------------------------------------

  TEST: Normal login with correct credentials
  Input username : alice
  Input password : alice123
  [SQL SENT] SELECT * FROM users WHERE username = 'alice' AND password = 'alice123'
  RESULT: LOGIN SUCCESS — Welcome, alice!

  TEST: Normal login with wrong password
  Input username : alice
  Input password : wrongpassword
  [SQL SENT] SELECT * FROM users WHERE username = 'alice' AND password = 'wrongpassword'
  RESULT: LOGIN FAILED — No matching account found.

  TEST: ATTACK: Bypass login with OR 1=1 injection
  Input username : ' OR '1'='1'--
  Input password : anything
  [SQL SENT] SELECT * FROM users WHERE username = '' OR '1'='1'--' AND password = 'anything'
  RESULT: LOGIN SUCCESS — Welcome, alice!

  TEST: ATTACK: Bypass password check using SQL comment
  Input username : admin'--
  Input password : anything
  [SQL SENT] SELECT * FROM users WHERE username = 'admin'--' AND password = 'anything'
  RESULT: LOGIN SUCCESS — Welcome, admin!
```

**What is happening here:**

- Test 1 and 2 are normal logins. They work correctly.
- Test 3 is an attack. The attacker typed `' OR '1'='1'--` as the username.
  The `'1'='1'` condition is always true. The `--` removes the password check.
  The database returns the first user it finds. Login succeeds without any real credentials.
- Test 4 is an attack. The attacker typed `admin'--` as the username.
  The `--` turns the password check into a comment. The database ignores it.
  Login succeeds without knowing the admin password.

Both attacks succeeded because the developer pasted user input directly into
the SQL string. The database could not tell the difference between the
developer's SQL and the attacker's input.

---

### Part 2 — Secure Login (the careful developer)

```
-----------------------------------------------------------------
  PART 2 — SECURE LOGIN (PreparedStatement)
-----------------------------------------------------------------

  TEST: Normal login with correct credentials
  Input username : alice
  Input password : alice123
  [SQL SENT] SELECT * FROM users WHERE username = ? AND password = ?
  [PARAM 1 - username] alice
  [PARAM 2 - password] ********
  RESULT: LOGIN SUCCESS — Welcome, alice!

  TEST: Normal login with wrong password
  Input username : alice
  Input password : wrongpassword
  [SQL SENT] SELECT * FROM users WHERE username = ? AND password = ?
  [PARAM 1 - username] alice
  [PARAM 2 - password] ********
  RESULT: LOGIN FAILED — No matching account found.

  TEST: ATTACK: Bypass login with OR 1=1 injection
  Input username : ' OR '1'='1'--
  Input password : anything
  [SQL SENT] SELECT * FROM users WHERE username = ? AND password = ?
  [PARAM 1 - username] ' OR '1'='1'--
  [PARAM 2 - password] ********
  RESULT: LOGIN FAILED — No matching account found.

  TEST: ATTACK: Bypass password check using SQL comment
  Input username : admin'--
  Input password : anything
  [SQL SENT] SELECT * FROM users WHERE username = ? AND password = ?
  [PARAM 1 - username] admin'--
  [PARAM 2 - password] ********
  RESULT: LOGIN FAILED — No matching account found.
```

**What is happening here:**

- Test 1 and 2 still work correctly for normal users.
- Test 3 and 4 are the same attack inputs — but this time both are blocked.

Notice the `[SQL SENT]` line never changes in Part 2:
```
SELECT * FROM users WHERE username = ? AND password = ?
```

The SQL structure is always fixed. The attack input is passed separately as
plain data. The database searches for a user whose username is literally
the text `' OR '1'='1'--` — no such user exists — so the login fails.

---

## Why the vulnerable version fails

The vulnerable login builds its SQL like this:

```java
String query = "SELECT * FROM users WHERE username = '" + username + "'";
```

The user input is glued directly into the SQL string using the `+` operator.
The database receives one combined string and cannot tell which part is the
developer's SQL and which part is the user's input.

---

## Why the secure version works

The secure login uses PreparedStatement:

```java
String query = "SELECT * FROM users WHERE username = ? AND password = ?";
pstmt.setString(1, username);
pstmt.setString(2, password);
```

The SQL structure is sent to the database first as a fixed template.
The user input is sent separately as data values only.
No matter what the user types, it can never change the SQL structure.

---

## Project structure

```
NCSA-SQL-Injection/
├── pom.xml                        <- Maven config and PostgreSQL driver dependency
├── .gitignore                     <- excludes db.properties and target folder
├── README.md                      <- this file
└── src/main/
    ├── resources/
    │   └── db.properties          <- your local database credentials (not committed)
    └── java/com/ncsa/sqldemo/
        ├── Main.java              <- runs all test cases against both logins
        ├── DatabaseSetup.java     <- creates the users table and inserts dummy accounts
        ├── VulnerableLogin.java   <- unsafe login using string concatenation
        └── SecureLogin.java       <- safe login using PreparedStatement
```

---

## Dummy accounts created automatically

Every time the program runs, it creates these accounts fresh:

| Username | Role  |
|----------|-------|
| alice    | user  |
| bob      | user  |
| admin    | admin |

Passwords are plain text for readability in this demo only.
Real applications must hash passwords using BCrypt or Argon2.

---

## Technologies used

- Java 17
- PostgreSQL
- JDBC — PostgreSQL driver 42.7.12
- Apache Maven 3.x
