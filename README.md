# Timecom Session Tracker

A comprehensive session tracking system built with Java Spring Boot and React.

## Project Architecture

- **Backend:** Spring Boot application providing RESTful APIs for authentication, robust session management, rate limiting, and analytics.
- **Frontend:** React application built with Vite and Tailwind CSS for visualizing session data and user activity.

## System Prerequisites

Before running the application, ensure you have the following installed and running on your system:

- **Java 21+** (for the Spring Boot backend)
- **Node.js** (v18+ recommended) & **npm** (for the React frontend)
- **PostgreSQL**: Must be running on `localhost:5432`. Ensure a database named `session_tracker` exists with user/password as `postgres`/`postgres` (can be configured in `application.yml`).
- **Redis**: Must be running on `localhost:6379` for session caching and rate-limiting.

## Project Dependencies

### Backend Dependencies (Maven)
- **Spring Boot 3.x**
  - Spring Web (REST APIs)
  - Spring Data JPA (Database access)
  - Spring Data Redis (Session caching)
  - Spring Security (Authentication)
  - Spring Validation
- **PostgreSQL Driver** (Database connectivity)
- **High-Performance Redis Lettuce Client**
- **Flyway** (Database migrations)
- **JJWT** (JSON Web Tokens integration)
- **Lombok** (Boilerplate reduction)

### Frontend Dependencies (npm/package.json)
- **React 19 & React DOM**
- **Vite** (Build tool and dev server)
- **Tailwind CSS & Autoprefixer** (Styling)
- **React Router DOM** (Navigation)
- **Axios** (HTTP Client)
- **Zustand** (State management)
- **Recharts** (Data visualization)
- **Lucide React** (Icons)

## Running the Backend

1. Navigate to the root directory `timecom`.
2. Run the backend application using the provided Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
The server will start on **http://localhost:8080**. Note that database schemas are automatically managed by Flyway migrations on startup.

## Running the Frontend

1. Navigate to the frontend directory:
   ```bash
   cd react-frontend
   ```
2. Install the necessary dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
The frontend will typically start on **http://localhost:5174** (or `5173`).
