# Airline Booking System - Postman Test Suite

This directory contains the Postman Collection and Environment files covering every endpoint and test scenario in the Airline Booking System microservices backend.

## Files

- [Airline_Booking_System.postman_collection.json](Airline_Booking_System.postman_collection.json): Comprehensive Postman Collection (v2.1.0) with automated test scripts and pre-request scripts.
- [Airline_Booking_System.postman_environment.json](Airline_Booking_System.postman_environment.json): Environment configuration for local development.

---

## Architecture & Gateway Routing

All calls are routed through the **API Gateway** on port `8080` (or `{{baseUrl}}`):

| Service                | Port (Direct) | Gateway Path                                               | Role Required                          |
| ---------------------- | ------------- | ---------------------------------------------------------- | -------------------------------------- |
| API Gateway & Actuator | `8080`        | `/actuator/**`, `/fallback/**`                             | Public                                 |
| Auth Service           | `8082`        | `/api/auth/**`                                             | Public (Register, Login, Refresh)      |
| Flight Service         | `8083`        | `/api/flights/**` (Search, Get)<br>`/api/flights/admin/**` | Public<br>`ROLE_ADMIN`                 |
| Passenger Service      | `8084`        | `/api/passengers/**`                                       | `ROLE_PASSENGER` / Authenticated       |
| Booking Service        | `8085`        | `/api/bookings/**`                                         | `ROLE_PASSENGER` / Authenticated       |
| Payment Service        | `8086`        | `/api/payments/initiate`<br>`/api/payments/webhook`        | Authenticated<br>Webhook Secret Header |
| Notification Service   | `8087`        | `/api/notifications/**`                                    | `ROLE_PASSENGER` / Authenticated       |
| Admin Service          | `8088`        | `/api/admin/**`                                            | `ROLE_ADMIN`                           |

---

## Test Suites Breakdown

### 01 - Authentication Service (`/api/auth`)

- **1.1 Register Passenger Account (Positive - 201)**: Generates a dynamic email, registers a passenger account, and extracts `accessToken`, `refreshToken`, and `userId`.
- **1.2 Register Duplicate Email (Negative - 400)**: Asserts duplicate email registration is rejected with 400 Bad Request.
- **1.3 Register Validation Failure (Negative - 400)**: Asserts rejection of blank names, invalid email format, and password under 8 characters.
- **1.4 Login User (Positive - 200)**: Verifies user login and updates active JWT access/refresh tokens.
- **1.5 Login Invalid Credentials (Negative - 401)**: Tests bad credentials response.
- **1.6 Refresh Access Token (Positive - 200)**: Exchanges refresh token for a newly signed access token.
- **1.7 Refresh Token with Invalid Token (Negative - 401)**: Tests rejection of revoked or corrupted refresh tokens.

### 02 - Flight Service (`/api/flights`)

- **2.1 Admin - Create Scheduled Flight (Positive - 201)**: Creates a new flight schedule and saves `flightId`.
- **2.2 Admin - Create Flight As Passenger (Negative - 403)**: Asserts that non-admin accounts cannot access `/api/flights/admin/**`.
- **2.3 Admin - Update Flight Schedule (Positive - 200)**: Modifies fare and available seats.
- **2.4 Search Flights (Public - Positive - 200)**: Searches flights by origin, destination, date, and passenger count.
- **2.5 Search Flights Invalid IATA Code (Negative - 400)**: Validates that airport codes must be 3 letters.
- **2.6 Get Flight by ID (Public - Positive - 200)**: Fetches flight details by ID.
- **2.7 Get Non-existent Flight by ID (Negative - 404)**: Asserts 404 response format.

### 03 - Passenger Service (`/api/passengers`)

- **3.1 Create Traveller Profile (Positive - 201)**: Creates a traveller profile and saves `passengerId`.
- **3.2 Create Traveller Without Auth Token (Negative - 401)**: Asserts 401 Unauthorized for unauthenticated requests.
- **3.3 Create Traveller Validation Error (Negative - 400)**: Tests future Date of Birth and blank name validation.
- **3.4 Get My Travellers (Positive - 200)**: Asserts listing travellers belonging to authenticated user.
- **3.5 Update Traveller Profile (Positive - 200)**: Updates passenger details.
- **3.6 Update Non-existent Traveller (Negative - 404)**: Asserts 404 response.

### 04 - Booking Service (`/api/bookings`)

- **4.1 Create Booking & Acquire Seat Lock (Positive - 201)**: Books seat in `PENDING_PAYMENT` state with Redis seat lock, extracting `bookingId` and `pnr`.
- **4.2 Create Booking Duplicate Seat Lock (Negative - 409)**: Confirms seat locking prevents concurrent duplicate booking of the same seat.
- **4.3 Create Booking Invalid Seat Number Format (Negative - 400)**: Validates seat regex pattern.
- **4.4 Get My Bookings (Positive - 200)**: Lists user bookings.
- **4.5 Get Booking by ID (Positive - 200)**: Fetches booking details.
- **4.6 Cancel Pending Booking (Positive - 200)**: Cancels pending booking and releases Redis seat lock.

### 05 - Payment Service (`/api/payments`)

- **5.1 Initiate Payment (Positive - 201)**: Creates payment intent and extracts `providerReference` and `paymentId`.
- **5.2 Initiate Payment Without Auth (Negative - 401)**: Asserts token requirement.
- **5.3 Webhook - Payment Succeeded Callback (Positive - 200)**: Simulates provider payment success callback.
- **5.4 Webhook - Payment Failed Callback (Positive - 200)**: Simulates provider payment failure callback.
- **5.5 Webhook - Invalid Secret (Negative - 401)**: Asserts webhook secret validation.
- **5.6 Webhook - Non-existent Payment Ref (Negative - 404)**: Asserts 404 for unknown provider reference.

### 06 - Notification Service (`/api/notifications`)

- **6.1 Get My Notification Delivery Logs (Positive - 200)**: Retrieves RabbitMQ notification delivery logs.
- **6.2 Get Notifications Without Auth (Negative - 401)**: Asserts unauthenticated access is blocked.

### 07 - Admin Service (`/api/admin`)

- **7.1 Get Operations Dashboard (Positive - 200)**: Fetches operational metrics as Admin.
- **7.2 Get Operations Dashboard As Passenger (Negative - 403)**: Asserts 403 Forbidden for non-admin accounts.
- **7.3 Record Operational Action (Positive - 202)**: Records audit log entry.

### 08 - API Gateway & Actuator

- **8.1 Flight Service Fallback Handler (Positive - 503)**: Tests Circuit Breaker fallback response.
- **8.2 Actuator Health (Positive - 200)**: Validates `status: "UP"`.
- **8.3 Actuator Info (Positive - 200)**: Checks gateway info.

### 09 - E2E Complete Booking Journey Flow

An 8-step sequential workflow that registers a user, creates a traveller profile, searches flights, reserves a seat, initiates payment, triggers the webhook callback, verifies the confirmed booking, and inspects notification event delivery.

---

## How to Run

### 1. In Postman App

1. Open Postman -> **Import** -> Select both files in the `postman/` directory:
   - `Airline_Booking_System.postman_collection.json`
   - `Airline_Booking_System.postman_environment.json`
2. Select the **Airline Booking System - Local Dev** environment in the top-right environment selector.
3. Click the collection -> Click **Run collection** -> Run with all tests.

### 2. Using Newman (CLI)

```bash
npm install -g newman

newman run postman/Airline_Booking_System.postman_collection.json \
  -e postman/Airline_Booking_System.postman_environment.json \
  --reporters cli,junit \
  --reporter-junit-export newman-report.xml
```
