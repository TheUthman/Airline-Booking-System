# API Endpoints Reference for Airline‑Booking‑System

This document lists every public HTTP endpoint exposed by the micro‑services in the repository, the HTTP method, the URL pattern, a short description of its purpose, and the expected request/response payloads where relevant.  It is intended as a quick cheat‑sheet for developers, testers, and any client (Postman, curl, frontend) that needs to call the backend.

---

## Auth Service (`authservice`)
| Method | URL | Description | Request Body (JSON) | Success Response |
|--------|-----|-------------|---------------------|------------------|
| `POST` | `/api/auth/register` | Register a new user and issue JWT + refresh token. | `{"firstName":"...","lastName":"...","email":"...","password":"..."}` | `201 Created` → `{ "accessToken": "...", "refreshToken": "...", "userId": 123, "role": "USER" }` |
| `POST` | `/api/auth/login` | Authenticate with email/password and receive JWT + refresh token. | `{"email":"...","password":"..."}` | `200 OK` → same shape as register response |
| `POST` | `/api/auth/refresh` | Exchange a refresh token for a new access token. | `{"refreshToken":"..."}` | `200 OK` → `{ "accessToken": "...", "refreshToken": "..." }` |
| `POST` | `/api/auth/promote/{userId}` | Promote an existing user to **ADMIN** role (admin‑only operation). | *none* | `200 OK` → `{ "message":"User promoted to ADMIN", "userId":123, "email":"...", "role":"ADMIN" }` |

---

## Pricing Service (`pricing-service`)
| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `POST` | `/api/pricing/quote` | Calculate a dynamic quote without changing the flight's stored base fare. The calculator applies advance-purchase, load-factor, cabin, promo-code, and frequent-flyer-point rules. | `{"flightId":123,"baseFare":199.99,"departureDate":"2026-10-01","availableSeats":30,"totalSeats":180,"cabin":"ECONOMY","promoCode":"WELCOME10","frequentFlyerPoints":500}` | `200 OK` → base fare, applied multiplier, discounts, total, and rules used. |

Pricing rules: departures within 3 days add 35%; 4–14 days add 15%; 65%/85%+ occupied flights add 10%/25%; BUSINESS and FIRST class apply 1.80× and 2.75× multipliers. `WELCOME10` grants 10%, and every 100 frequent-flyer points offsets one currency unit, capped at 100.

---

## Passenger Service (`passenger-service`)
| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `GET` | `/api/passengers/me` | Return all passengers owned by the caller (identified by `X-User-Email` header). | – | `200 OK` → list of passenger objects |
| `POST` | `/api/passengers` | Create a new passenger profile. | `{ "firstName":"...","lastName":"...","dateOfBirth":"YYYY‑MM‑DD","phone":"...","documentNumber":"..." }` | `201 Created` → created passenger object |
| `PUT` | `/api/passengers/{id}` | Update an existing passenger (owner only). | Same shape as POST | `200 OK` → updated passenger |

---

## Booking Service (`booking-service`)
| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `GET` | `/api/bookings` | List all bookings for the caller (`X-User-Email`). | – | `200 OK` → list of bookings |
| `GET` | `/api/bookings/{id}` | Retrieve a single booking owned by the caller. | – | `200 OK` → booking object |
| `POST` | `/api/bookings` | Create a new booking and **lock** a seat for 10 min. | `{ "flightId":123, "passengerId":456, "seatNumber":"12A", "amount":199.99 }` | `201 Created` → newly created booking (status **PENDING_PAYMENT**) |
| `POST` | `/api/bookings/{id}/cancel` | Cancel a pending booking (cannot cancel a confirmed one). | – | `200 OK` → cancelled booking |

---

## Payment Service (`payment-service`)
| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `POST` | `/api/payments/initiate` | Create a **pending** payment record for a booking. | `{ "bookingId":123, "amount":199.99 }` (header `X-User-Email` identifies payer) | `201 Created` → payment object with provider reference |
| `POST` | `/api/payments/webhook` | Endpoint called by the payment provider to report success/failure. | `{ "providerReference":"pay_...", "succeeded":true }` (header `X-Payment-Webhook-Secret` for auth) | `200 OK` (no body) – also publishes an event (`payment.succeeded` / `payment.failed`). |

---

## Notification Service (`notification-service`)
### Generic Notification API
| Method | URL | Description | Request Header | Success Response |
|--------|-----|-------------|----------------|------------------|
| `GET` | `/api/notifications` | Fetch the 50 most recent notifications for the logged‑in user. | `X-User-Email: user@example.com` | `200 OK` → list of `Notification` objects |

### Email‑specific Controllers (used internally but also callable for testing)
| Controller | Method | URL | Description |
|------------|--------|-----|-------------|
| `AccountNotificationController` | `POST` | `/api/notifications/account` | Send *account verification* email (template `account‑verification.html`). Expected JSON: `{ "recipientEmail":"...", "firstName":"...", "verificationUrl":"https://..." }` |
| `BookingNotificationController` | `POST` | `/api/notifications/booking` | Send *booking confirmation* email. Payload includes `bookingId`, `recipientEmail`, etc. |
| `FlightNotificationController` | `POST` | `/api/notifications/flight` | Send *flight schedule update* email. |
| `PaymentNotificationController` | `POST` | `/api/notifications/payment` | Send *payment receipt* email. |
| `NotificationController` (generic) | `GET` | `/api/notifications` | Already listed above – fetch stored notifications. |

---

## Flight Service (`flight-service`)
| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `GET` | `/api/flights/search` | Search for active flights matching origin, destination, date and required seats. | Query params: `origin`, `destination`, `date` (ISO), `passengers` (default 1) | `200 OK` → list of flight objects |
| `GET` | `/api/flights/{id}` | Retrieve flight details by ID. | – | `200 OK` → flight object |
| `POST` | `/api/flights/admin` | **Admin** – create a new flight record. | `{ "flightNumber":"AB123", "origin":"JFK", "destination":"LAX", "departureTime":"2026-10-01T08:00", "arrivalTime":"2026-10-01T11:00", "fare":199.99, "availableSeats":150 }` | `201 Created` → saved flight |
| `PUT` | `/api/flights/admin/{id}` | **Admin** – update an existing flight. | Same shape as POST | `200 OK` → updated flight |

---

## Admin Service (`admin-service`)
| Method | URL | Description |
|--------|-----|-------------|
| `GET` | `/api/admin/users` | List all registered users (admin‑only). |
| `PUT` | `/api/admin/users/{id}/role` | Change a user's role (e.g., promote to ADMIN). |
| `DELETE` | `/api/admin/users/{id}` | Delete a user account. |

---

## Common Header
All services expect the **caller’s email** to be passed via the HTTP header `X-User-Email`.  This header is populated by the API‑gateway after the JWT has been validated.

---

### How to Test Quickly with `curl`
```bash
# 1️⃣ Register a user (auth)
curl -X POST http://localhost:8081/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"firstName":"John","lastName":"Doe","email":"john@example.com","password":"Secret123!"}'

# 2️⃣ Login – capture the access token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"john@example.com","password":"Secret123!"}' | jq -r .accessToken)

# 3️⃣ Search flights (flight‑service)
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-User-Email: john@example.com" \
     "http://localhost:8083/api/flights/search?origin=JFK&destination=LAX&date=2026-10-01"

# 4️⃣ Book a seat (booking‑service)
curl -X POST http://localhost:8084/api/bookings \
     -H "Content-Type: application/json" \
     -H "X-User-Email: john@example.com" \
     -d '{"flightId":1,"passengerId":1,"seatNumber":"12A","amount":199.99}'

# 5️⃣ Initiate payment (payment‑service)
curl -X POST http://localhost:8085/api/payments/initiate \
     -H "Content-Type: application/json" \
     -H "X-User-Email: john@example.com" \
     -d '{"bookingId":1,"amount":199.99}'

# 6️⃣ Verify email was sent (MailHog UI at http://localhost:8025)
```

---

*This file is generated for documentation purposes and kept up‑to‑date manually. Feel free to add more endpoints as the project evolves.*
