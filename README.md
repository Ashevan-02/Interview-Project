# SQL Injection Demo

A Java console application that demonstrates how an SQL injection attack happens
and how it can be prevented using PreparedStatement.


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
