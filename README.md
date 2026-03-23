# Timecom Session Tracker

A comprehensive session tracking system built with Java Spring Boot and React.

## Project Architecture

- **Backend:** Spring Boot application providing RESTful APIs for authentication, robust session management, rate limiting, and analytics.
- **Frontend:** React application built with Vite and Tailwind CSS for visualizing session data and user activity.

## Prerequisites

Before running the application, ensure you have the following installed and running:

- **Java 21+** (for the Spring Boot backend)
- **Node.js** (for the React frontend)
- **PostgreSQL**: Must be running on `localhost:5432`. Ensure a database named `session_tracker` exists with user/password as `postgres`/`postgres` (can be configured in `application.yml`).
- **Redis**: Must be running on `localhost:6379` for session caching and rate-limiting.

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
