# Airline Booking System Backend

Spring Boot microservices implementation of the supplied architecture guide. The gateway is the only backend URL a frontend should call.

The root `pom.xml` is a Maven aggregator, so you can compile all services from the project root with `mvn compile` or package them with `mvn package`.

API errors use the same timestamp/status/error/message/path response shape.

The gateway API contract is available as [OpenAPI 3.0 YAML](docs/openapi.yaml); import it into Swagger Editor, Postman, or another OpenAPI client to view and exercise the documented endpoints.

## Services

| Service | Port | Responsibility |
| --- | ---: | --- |
| API Gateway | 8080 | JWT validation, routing and role enforcement |
| Service Registry | 8761 | Eureka service discovery |
| Config Server | 8888 | Central configuration endpoint |
| Auth Service | 8082 | Registration, login and refresh tokens |
| Flight Service | 8083 | Flight search and admin flight schedules |
| Passenger Service | 8084 | Traveller profiles and documents |
| Booking Service | 8085 | PNRs, pending reservations and Redis seat locks |
| Payment Service | 8086 | Payment intents, webhook verification and payment events |
| Notification Service | 8087 | RabbitMQ event delivery logs |
| Admin Service | 8088 | Protected operational endpoints |

## Local setup

1. Copy `.env.example` values into your environment. Set a unique `JWT_SECRET` of at least 32 characters.
2. Start the complete local platform with `docker compose up --build`, or start only Redis and RabbitMQ with `docker compose up -d redis rabbitmq`. RabbitMQ management is at `http://localhost:15672` (user/password `airline` / `airline_local_password`).
3. Start `config-server`, then `service-registry`, followed by the gateway and the business services. Each folder can be started with `mvn spring-boot:run`.
4. Use `http://localhost:8080` from the frontend. Do not send requests directly to a business-service port.

The business services default to local file-backed H2 databases for zero-setup development. Set each `*_DB_URL`, `*_DB_USERNAME`, and `*_DB_PASSWORD` variable to PostgreSQL in deployed environments. Every service uses its own database/schema; never share tables across services.

Set `CORS_ALLOWED_ORIGINS` to the exact Vercel or Netlify frontend URL when deploying.

## Key API flow

1. `POST /api/auth/register` or `/api/auth/login` returns access and refresh tokens.
2. Send `Authorization: Bearer <access-token>` for protected requests.
3. Search with `GET /api/flights/search?origin=LOS&destination=ABV&date=2026-09-01&passengers=1`.
4. Create passenger profiles at `POST /api/passengers`, then a pending reservation at `POST /api/bookings`.
5. Create a payment at `POST /api/payments/initiate`. A trusted provider callback calls `POST /api/payments/webhook`, which publishes `payment.succeeded` or `payment.failed` to RabbitMQ. Booking Service confirms or releases the reservation accordingly.

The webhook endpoint is intentionally not a frontend endpoint. Replace the development callback validation with the payment provider's signed-webhook verification before production.

## Security notes

- Gateway makes flight search and Auth register/login/refresh public; all other routes require a valid access token.
- `/api/flights/admin/**` and `/api/admin/**` require the `ADMIN` role.
- The gateway forwards authenticated identity as internal headers. Keep business-service ports private in production so callers cannot forge those headers.
- Store production secrets in deployment environment settings, not in this repository.
