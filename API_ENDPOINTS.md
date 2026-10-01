# API Endpoints Reference for Airline-Booking-System

This document lists every HTTP endpoint exposed by the micro-services in the repository and how to reach it through the **API Gateway** (`http://localhost:8080`). The gateway is the only backend URL a frontend should call; it validates JWTs, enforces ADMIN roles and routes requests to the business services.

## Conventions

- **Authentication** - Protected endpoints require `Authorization: Bearer <accessToken>`. Public endpoints (register, login, refresh, password-reset / verification, payment initiate / webhook, flight search, fallback) do not.
- **Caller identity** - After JWT validation the gateway sets the `X-User-Email` and `X-User-Role` headers used by the services. Clients do not send these headers manually.
- **Admin rights** - Routes marked **ADMIN** require the JWT role to be `ADMIN` (enforced by the gateway; a non-admin receives `403 Forbidden`).
- **Errors** - All services return the same error shape: `{ "timestamp": "...", "status": 400, "error": "...", "message": "...", "path": "..." }`.

### Security matrix (per endpoint group)

| Group | Access |
|-------|--------|
| `/api/auth/register` / `/login` / `/refresh` / `/forgot-password` / `/reset-password` / `/verification` / `/verification/confirm` | Public |
| `/api/payments/initiate` / `/api/payments/webhook` | Public (webhook additionally requires the `X-Payment-Webhook-Secret` header) |
| `GET /api/flights/**` (search, by id) | Public |
| `GET /fallback/flight-service`, `/actuator/**`, `OPTIONS /**` | Public |
| `/api/auth/promote/**` / `/api/flights/admin/**` / `/api/admin/**` | **ADMIN** only |
| Everything else (`/api/passengers/**`, `/api/bookings/**`, `POST /api/pricing/quote`, payment history/refund/invoice, `/api/notifications/**`) | Authenticated (Bearer token) |

> `POST /api/payments/initiate` is reachable without a token at the gateway, but the payer is taken from `X-User-Email` (injected from the JWT), so a Bearer token is still required for the payment to belong to a real account.

## Auth Service (`authservice`)

| Method | URL | Description | Request Body (JSON) | Success Response |
|--------|-----|-------------|---------------------|------------------|
| `POST` | `/api/auth/register` | Register a new user and issue access + refresh tokens. | `{ "firstName":"...","lastName":"...","email":"...","password":"..." }` (password >= 8 chars) | `201 Created` -> `{ "token":"...","tokenType":"Bearer","userId":123,"firstName":"...","lastName":"...","email":"...","role":"PASSENGER","refreshToken":"..." }` |
| `POST` | `/api/auth/login` | Authenticate with email/password and receive tokens. | `{ "email":"...","password":"..." }` | `200 OK` -> same shape as register response |
| `POST` | `/api/auth/refresh` | Exchange a refresh token for a new access token. | `{ "refreshToken":"..." }` | `200 OK` -> same shape as register response |
| `POST` | `/api/auth/promote/{userId}` | **ADMIN** - promote an existing user to `ADMIN`. | - | `200 OK` -> `{ "message":"User promoted to ADMIN","userId":123,"email":"...","role":"ADMIN" }` |
| `POST` | `/api/auth/forgot-password` | Public - request a password-reset email for the account (if it exists). | `{ "email":"..." }` | `202 Accepted` -> `{ "message":"If the account exists, a reset email will be sent" }` |
| `POST` | `/api/auth/reset-password` | Public - set a new password using the token emailed to the user. | `{ "token":"...","password":"..." }` | `204 No Content` |
| `POST` | `/api/auth/verification` | Public - request the account-verification email. | `{ "email":"..." }` | `202 Accepted` -> `{ "message":"Verification email requested" }` |
| `POST` | `/api/auth/verification/confirm` | Public - verify the account with the emailed token. | `{ "token":"..." }` | `204 No Content` |

## Pricing Service (`pricing-service`)

| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `POST` | `/api/pricing/quote` | Calculate a dynamic quote without changing the stored base fare (advance-purchase, load-factor, cabin, promo-code and frequent-flyer-point rules). | `{ "flightId":123,"baseFare":199.99,"departureDate":"2026-10-01","availableSeats":30,"totalSeats":180,"cabin":"ECONOMY","promoCode":"WELCOME10","frequentFlyerPoints":500 }` | `200 OK` -> `{ "flightId":123,"cabin":"ECONOMY","baseFare":199.99,"multiplier":1.0,"promoDiscount":19.99,"frequentFlyerPointsDiscount":5.00,"total":175.00,"appliedRules":["..."] }` |

Pricing rules: departures within 3 days add 35%; 4-14 days add 15%; flights 65% / 85%+ occupied add 10% / 25%; BUSINESS and FIRST cabins apply 1.80x and 2.75x multipliers. `WELCOME10` grants 10%, and every 100 frequent-flyer points offsets one currency unit (capped at 100).

## Passenger Service (`passenger-service`)

| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `GET` | `/api/passengers/me` | List the caller's traveller profiles. | - | `200 OK` -> list of passenger objects |
| `GET` | `/api/passengers/saved-travelers` | List only the caller's travellers flagged as saved. | - | `200 OK` -> list of passenger objects |
| `POST` | `/api/passengers` | Create a traveller profile. | `{ "firstName":"...","lastName":"...","dateOfBirth":"YYYY-MM-DD","phone":"...","documentNumber":"...","passportNationality":"...","passportExpiryDate":"YYYY-MM-DD","savedTraveler":true }` | `201 Created` -> created passenger object |
| `PUT` | `/api/passengers/{id}` | Update a passenger (owner only). | Same shape as POST | `200 OK` -> updated passenger |
| `POST` | `/api/passengers/{id}/points` | Add frequent-flyer points to a passenger (owner only). | Query param `points` (>= 1) | `200 OK` -> updated passenger with new `frequentFlyerPoints` |

Passenger object: `{ "id", "ownerEmail", "firstName", "lastName", "dateOfBirth", "phone", "documentNumber", "passportNationality", "passportExpiryDate", "frequentFlyerPoints", "savedTraveler" }`

## Booking Service (`booking-service`)

| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `GET` | `/api/bookings` | List the caller's bookings. | - | `200 OK` -> list of bookings |
| `GET` | `/api/bookings/{id}` | Retrieve one of the caller's bookings. | - | `200 OK` -> booking object |
| `POST` | `/api/bookings` | Create a booking and **lock** the seat for 10 min (Redis). | `{ "flightId":123,"passengerId":456,"seatNumber":"12A","amount":199.99 }` | `201 Created` -> booking with status `PENDING_PAYMENT`; `409 Conflict` if the seat is locked |
| `POST` | `/api/bookings/group` | Create one pending booking per traveller (each seat locked independently). | `{ "flightId":123,"travelers":[ { "passengerId":1,"seatNumber":"12A","amount":199.99 }, { "passengerId":2,"seatNumber":"12B","amount":199.99 } ] }` | `201 Created` -> list of bookings |
| `POST` | `/api/bookings/{id}/upgrade` | Change a pending booking's seat and add the price difference. | Query params `seatNumber` and `additionalAmount` | `200 OK` -> updated booking |
| `POST` | `/api/bookings/{id}/cancel` | Cancel a pending booking (confirmed bookings require support). | - | `200 OK` -> booking with status `CANCELLED` |

Booking object: `{ "id", "pnr", "ownerEmail", "flightId", "passengerId", "seatNumber", "amount", "status" ("PENDING_PAYMENT"|"CONFIRMED"|"CANCELLED"|"EXPIRED"), "createdAt" }`

## Payment Service (`payment-service`)

| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `POST` | `/api/payments/initiate` | Create a **pending** payment record for a booking. | `{ "bookingId":123,"amount":199.99 }` | `201 Created` -> payment object with `providerReference` |
| `GET` | `/api/payments` | List the caller's payment history. | - | `200 OK` -> list of payments |
| `POST` | `/api/payments/{id}/refund` | Refund a succeeded payment. | - | `200 OK` -> payment with status `REFUNDED`; `409 Conflict` if not `SUCCEEDED` |
| `GET` | `/api/payments/{id}/invoice` | View a payment invoice. | - | `200 OK` -> `{ "invoiceNumber","paymentReference","bookingId","amount","status","issuedAt" }` |
| `POST` | `/api/payments/webhook` | Internal - provider callback reporting success/failure; publishes `payment.succeeded` / `payment.failed` to RabbitMQ. | `{ "providerReference":"pay_...","succeeded":true }` + header `X-Payment-Webhook-Secret` | `200 OK` (empty body); `401 Unauthorized` for a bad secret |

Payment object: `{ "id", "providerReference", "bookingId", "ownerEmail", "amount", "status" ("PENDING"|"SUCCEEDED"|"FAILED"|"REFUNDED"), "createdAt" }`

## Notification Service (`notification-service`)

| Method | URL | Description | Request Header | Success Response |
|--------|-----|-------------|----------------|------------------|
| `GET` | `/api/notifications` | Fetch the 50 most recent delivery logs for the caller. | `X-User-Email` (injected by the gateway) | `200 OK` -> list of `Notification` objects |

Email-dispatch endpoints (used internally; also callable for testing) all return `202 Accepted` -> `{ "message":"... queued for dispatch" }`.

| Controller | Method | URL | Required JSON fields |
|------------|--------|-----|----------------------|
| `AccountNotificationController` | `POST` | `/api/notifications/account/password-reset` | `{ "recipientEmail","resetUrl","userName","expireInMinutes" }` |
| `AccountNotificationController` | `POST` | `/api/notifications/account/verification` | `{ "recipientEmail","verificationUrl","userName" }` |
| `BookingNotificationController` | `POST` | `/api/notifications/booking/confirmation` | `{ "recipientEmail","bookingId","bookingReference","flightNumber","origin","destination","departureTime","arrivalTime","totalAmount","passengerNames" }` |
| `FlightNotificationController` | `POST` | `/api/notifications/flight/update` | `{ "recipientEmail","flightNumber","origin","destination","scheduledDeparture","newDeparture","gate","statusMessage" }` |
| `FlightNotificationController` | `POST` | `/api/notifications/flight/cancellation` | `{ "recipientEmail","flightNumber","origin","destination","departureDate","reason","refundPolicyUrl" }` |
| `PaymentNotificationController` | `POST` | `/api/notifications/payment/confirmation` | `{ "recipientEmail","bookingId","paymentId","providerReference","amount","paymentMethod" }` |

Notification object: `{ "id", "eventType", "recipient", "payload", "deliveryStatus", "deliveryError", "createdAt" }`

## Flight Service (`flight-service`)

| Method | URL | Description | Request Body / Params | Success Response |
|--------|-----|-------------|----------------------|------------------|
| `GET` | `/api/flights/search` | Search active flights. Query params: `origin` (3 letters, required), `destination` (3 letters, required), `date` (ISO), `passengers` (default 1); optional `airline`, `maxPrice`, `maxDurationMinutes`. | - | `200 OK` -> list of flight objects |
| `GET` | `/api/flights/{id}` | Retrieve flight details by ID. | - | `200 OK` -> flight object |
| `POST` | `/api/flights/admin` | **ADMIN** - create a flight. | `{ "flightNumber":"AB123","origin":"JFK","destination":"LAX","departureTime":"2026-10-01T08:00","arrivalTime":"2026-10-01T11:00","fare":199.99,"availableSeats":150,"totalSeats":180,"airline":"Sky High","aircraftCode":"B738" }` | `201 Created` -> saved flight |
| `PUT` | `/api/flights/admin/{id}` | **ADMIN** - update an existing flight. | Same shape as POST | `200 OK` -> updated flight |
| `POST` | `/api/flights/admin/{id}/delay` | **ADMIN** - delay a flight by N minutes. | Query param `minutes` (>= 1) | `200 OK` -> flight with status `DELAYED` |
| `POST` | `/api/flights/admin/{id}/cancel` | **ADMIN** - cancel a flight (deactivates it). | - | `200 OK` -> flight with `active=false`, status `CANCELLED` |

Flight object: `{ "id", "flightNumber", "origin", "destination", "departureTime", "arrivalTime", "fare", "availableSeats", "totalSeats", "airline", "aircraftCode", "status" ("SCHEDULED"|"DELAYED"|"CANCELLED"), "delayMinutes", "active" }`

### Airport & aircraft catalogue (**ADMIN**)

| Method | URL | Description | Success Response |
|--------|-----|-------------|------------------|
| `GET` | `/api/flights/admin/airports` | List airports. | `200 OK` -> list of `{ "id","code","name","city","country" }` |
| `POST` | `/api/flights/admin/airports` | Add an airport. | `201 Created` -> airport |
| `PUT` | `/api/flights/admin/airports/{id}` | Update an airport. | `200 OK` -> airport |
| `DELETE` | `/api/flights/admin/airports/{id}` | Delete an airport. | `204 No Content` |
| `GET` | `/api/flights/admin/aircraft` | List aircraft. | `200 OK` -> list of `{ "id","code","model","seatCapacity" }` |
| `POST` | `/api/flights/admin/aircraft` | Add an aircraft. | `201 Created` -> aircraft |
| `PUT` | `/api/flights/admin/aircraft/{id}` | Update an aircraft. | `200 OK` -> aircraft |
| `DELETE` | `/api/flights/admin/aircraft/{id}` | Delete an aircraft. | `204 No Content` |

Airport body: `{ "code":"LOS","name":"Murtala Muhammed Intl","city":"Lagos","country":"NG" }` - Aircraft body: `{ "code":"B738","model":"Boeing 737-800","seatCapacity":180 }`

## Admin Service (`admin-service`)

| Method | URL | Description | Request Body | Success Response |
|--------|-----|-------------|--------------|------------------|
| `GET` | `/api/admin/dashboard` | **ADMIN** - view the operational dashboard. | - | `200 OK` -> `{ "service":"admin-service","generatedAt":"...","auditActionCount":5,"message":"..." }` |
| `POST` | `/api/admin/actions` | **ADMIN** - record an administrative action (audit log). | Free-form JSON action object | `202 Accepted` -> `{ "requestedBy":"admin@example.com","action":{...},"status":"RECORDED" }` |

## API Gateway

| Method | URL | Description | Success Response |
|--------|-----|-------------|------------------|
| `GET` | `/fallback/flight-service` | Circuit-breaker fallback returned when flight-service is unhealthy. | `503 Service Unavailable` -> `{ "error":"Flight Service Unavailable","message":"..." }` |

## Common Header

All services expect the caller's email through the `X-User-Email` header. The API-gateway populates it (and `X-User-Role`) from the validated JWT, so clients only need to send `Authorization: Bearer <token>`.

---

### How to Test Quickly with `curl` (through the gateway on port 8080)

```bash
# 1. Register a user (public)
curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"firstName":"John","lastName":"Doe","email":"john@example.com","password":"Secret123!"}'

# 2. Login - capture the access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"john@example.com","password":"Secret123!"}' | jq -r .token)

# 3. Search flights (public)
curl "http://localhost:8080/api/flights/search?origin=JFK&destination=LAX&date=2026-10-01"

# 4. Create a traveller profile
curl -X POST http://localhost:8080/api/passengers \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"firstName":"John","lastName":"Doe","dateOfBirth":"1990-01-01","documentNumber":"AB123456"}'

# 5. Book a seat (locks the seat for 10 minutes)
curl -X POST http://localhost:8080/api/bookings \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"flightId":1,"passengerId":1,"seatNumber":"12A","amount":199.99}'

# 6. Initiate payment
curl -X POST http://localhost:8080/api/payments/initiate \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"bookingId":1,"amount":199.99}'

# 7. Verify email was sent (MailHog UI at http://localhost:8025)
```

---

*This file is generated for documentation purposes and kept up-to-date manually. Feel free to add more endpoints as the project evolves.*
