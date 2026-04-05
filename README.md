# Code-Collaboration-Platform-Backend

# 🔐 Auth Service — CodeSync

The **Auth Service** is the security gateway of the CodeSync platform. It handles user authentication, authorization, JWT token management, OAuth2 login integration, and user profile management.

---

## 🚀 Features

### 🔑 Authentication

* User Registration (Email & Password)
* User Login (JWT-based authentication)
* Secure password hashing using BCrypt

### 🪪 Authorization

* Role-based access control (`DEVELOPER`, `ADMIN`)
* JWT token generation and validation

### 🌐 OAuth2 Integration

* Login with Google
* Login with GitHub

### 👤 User Management

* Get user profile
* Update profile (username, avatar, bio)
* Change password
* Deactivate account

### 🔍 User Discovery

* Search users by username
* Fetch user by ID / Email

---

## 🧱 Tech Stack

* **Backend Framework:** Spring Boot
* **Security:** Spring Security + JWT
* **OAuth:** Spring OAuth2 Client
* **Database:** MYSQL
* **ORM:** Spring Data JPA
* **Build Tool:** Maven

---

## 📁 Project Structure

```
auth-service/
│
├── controller/        # REST Controllers
├── service/           # Business Interfaces
├── service/impl/      # Business Logic Implementation
├── repository/        # JPA Repositories
├── entity/            # Database Entities
├── dto/               # Request/Response DTOs
├── security/          # JWT + Security Config
├── config/            # App Configurations
└── AuthServiceApplication.java
```
---

## 🔐 JWT Flow

1. User logs in using `/auth/login`
2. Server returns a JWT token
3. Client sends token in header:

   ```
   Authorization: Bearer <token>
   ```
4. Token is validated before accessing protected APIs

---

## 🌐 OAuth2 Flow

1. User clicks "Login with Google/GitHub"
2. Redirected to OAuth provider
3. Provider sends user info
4. Auth Service:

   * Creates user if not exists
   * Generates JWT token
5. User is authenticated

---

## 🗄️ Database Schema (User)

| Field        | Type     | Description                |
| ------------ | -------- | -------------------------- |
| userId       | Long     | Primary Key                |
| username     | String   | Unique username            |
| email        | String   | Unique email               |
| passwordHash | String   | Encrypted password         |
| fullName     | String   | User full name             |
| role         | Enum     | DEVELOPER / ADMIN          |
| provider     | Enum     | LOCAL / GOOGLE / GITHUB    |
| avatarUrl    | String   | Profile image URL          |
| bio          | String   | User bio                   |
| isActive     | Boolean  | Account status             |
| createdAt    | DateTime | Account creation timestamp |

---

