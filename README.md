# Inventory Management App

A full-stack inventory management application with a React/Next.js frontend and a Spring Boot backend.

This repo contains:
- `frontend/` — Next.js app for inventory dashboard, authentication, products, and spaces
- `backend/inventory/` — Spring Boot REST API with JWT auth, Google OAuth, email OTP, and MySQL persistence

---

## 📋 Table of Contents

- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Architecture](#-architecture)
- [Installation](#-installation)
- [Running the App](#-running-the-app)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)
- [API Documentation](#-api-documentation)
- [Testing](#-testing)
- [Notes](#-notes)
- [License](#-license)

---

## ✨ Features

- Space-based inventory organization
- Product CRUD with stock and threshold validation
- Low stock alerts and dashboard insights
- JWT authentication and Google OAuth
- Email OTP verification for signup
- Audit history and activity tracking

---

## 🛠️ Technology Stack

- Frontend: **Next.js 15**, **React 19**, **TypeScript**, **Tailwind CSS**
- Backend: **Java 17**, **Spring Boot 3.5**, **Spring Security**, **Spring Data JPA**
- Database: **MySQL** (JDBC), compatible with other relational databases
- API docs: **Springdoc OpenAPI**

---

## 🏗️ Architecture

The repository is split into two main layers:

- `frontend/` — client application, pages, components, authentication flows, and dashboard UI
- `backend/inventory/` — Spring Boot REST API, security, persistence, email, and OAuth

The frontend communicates with the backend via REST API calls, and the backend persists data into the configured database.

---

## 🚀 Installation

### Prerequisites

- Java 17 or newer
- Maven 3.6+ (or use the included Maven wrapper)
- Node.js 18+ and npm
- MySQL or compatible relational database

### 1. Clone repository

```bash
git clone <repo-url>
cd Inventory-Management-App
```

### 2. Backend setup

```bash
cd backend/inventory
```

Windows:

```powershell
./mvnw.cmd clean package
./mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw clean package
./mvnw spring-boot:run
```

### 3. Frontend setup

```bash
cd ../../frontend
npm install
npm run dev
```

---

## ▶️ Running the App

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:4028`

> The frontend development server is configured to run on port `4028`.

---

## ⚙️ Configuration

Backend configuration is in `backend/inventory/src/main/resources/application.properties`.

Required configuration values:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `app.jwt.secret`
- `spring.mail.host`
- `spring.mail.port`
- `spring.mail.username`
- `spring.mail.password`
- `spring.security.oauth2.client.registration.google.client-id`
- `spring.security.oauth2.client.registration.google.client-secret`
- `app.oauth2.frontend-redirect-uri`

### Example backend config

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/inventory?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your-db-password

app.jwt.secret=change-this-to-a-secure-random-value

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@example.com
spring.mail.password=your-email-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
app.oauth2.frontend-redirect-uri=http://localhost:4028/oauth/callback
```

> Do not commit credentials or secrets to source control.

---

## 📁 Project Structure

- `backend/inventory/`
  - `pom.xml` — Maven project definition
  - `src/main/java/` — backend source code
  - `src/main/resources/application.properties` — backend runtime configuration
- `frontend/`
  - `package.json` — frontend dependencies and scripts
  - `src/` — Next.js application source
  - `public/` — static assets

---

## 📘 API Documentation

When the backend is running, API docs are available at:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

---

## 🧪 Testing

Backend:

```bash
cd backend/inventory
./mvnw.cmd test
```

Frontend:

```bash
cd frontend
npm run lint
npm run type-check
```

---

## 📌 Notes

- The frontend expects the backend API to be available at `http://localhost:8080`.
- OAuth callback is configured for `http://localhost:4028/oauth/callback`.
- The backend currently uses MySQL default connection settings in `application.properties`.

---

## 🤖 AI-Assisted Development

This project was developed with AI assistance as a productivity tool for tasks such as brainstorming, debugging, documentation, refactoring suggestions, and implementation guidance.

All architectural decisions, feature selection, integration, testing, and final code review were performed by me. The project was built as a learning exercise, and I understand and maintain the codebase myself.

---

## 📄 License

This project is licensed under the Apache License 2.0
