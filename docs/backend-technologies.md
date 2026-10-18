# Backend technologies and where they are used

This document describes the technologies declared in the backend project and
the role they play. The backend is a Java microservices application. Its
service dependencies and versions are declared in each service's `pom.xml`;
deployment dependencies are described in the Docker Compose files.

## Core development stack

| Technology              | Where it is used                                                                                                                                                                                                             |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Java 21                 | Runtime and language for the backend services.                                                                                                                                                                               |
| Spring Boot 4.1.0       | Application framework used by the independently deployable services. Spring MVC provides HTTP/JSON APIs; Spring Boot starters integrate web, persistence, validation, security, messaging, and health features where needed. |
| Spring Cloud 2025.1.2   | Shared version line for Spring Cloud components, including Gateway, Eureka, Config, and circuit-breaker integration.                                                                                                         |
| Apache Maven            | Build and dependency management. The root `pom.xml` aggregates the service modules; each service has its own `pom.xml` and Spring Boot packaging configuration.                                                              |
| Jakarta Bean Validation | Validates HTTP request parameters and request bodies, such as airport codes, dates, passenger counts, and booking/payment inputs. Used by API services that declare `spring-boot-starter-validation`.                        |
| Spring Boot Actuator    | Health and operational endpoints, including container health checks for the configuration server and service registry. Also declared by several business services.                                                           |

## Platform and infrastructure technologies

| Technology                           | Where it is used                                                                                                                                                                                                                                                                                         |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Spring Cloud Gateway (Server WebMVC) | `api-gateway` is the frontend-facing entry point. It routes `/api/...` requests to backend services and applies access-token, role, CORS, and rate-limit policies.                                                                                                                                       |
| Spring Security                      | Authentication/authorization integration in `authservice` and `api-gateway`; also declared by `flight-service` for its service-level security configuration.                                                                                                                                             |
| JSON Web Tokens (JJWT 0.13.0)        | Created and refreshed by `authservice`; validated by the gateway (and supported by the flight service's security dependencies/configuration). The gateway forwards authenticated identity/role information to internal services.                                                                         |
| Netflix Eureka                       | `service-registry` runs the Eureka server. Business services and the gateway register as Eureka clients so routes and services can be discovered by name.                                                                                                                                                |
| Spring Cloud Config                  | `config-server` serves shared and service-specific configuration from `config-server/src/main/resources/config-repo/`. Services use the Config client to retrieve settings such as ports, database connections, and downstream service URLs.                                                             |
| PostgreSQL 16                        | Primary relational database in the supplied Docker Compose environment and intended deployed database. Services with persistence own their own database/schema; for example, the Compose file configures separate databases for auth, flight, passenger, booking, payment, notification, and admin data. |
| Spring Data JPA / Hibernate          | Object-relational persistence for service-owned data in `authservice`, `flight-service`, `passenger-service`, `booking-service`, `payment-service`, `notification-service`, and `admin-service`.                                                                                                         |
| H2                                   | Embedded database dependency used for local/testing configurations in selected services. It is not the deployed database in the supplied Compose configuration; check a service's profile/configuration before relying on it.                                                                            |
| Flyway                               | Database migration tooling declared by `authservice` and `flight-service`, for versioned schema changes in those services.                                                                                                                                                                               |
| Redis 7                              | Used by `booking-service` for short-lived seat locks so concurrent bookings cannot claim the same seat simultaneously.                                                                                                                                                                                   |
| RabbitMQ 4 (management image)        | Asynchronous event transport. `payment-service` publishes payment result events; `booking-service` consumes them to confirm/release reservations; `notification-service` consumes notification events for delivery.                                                                                      |
| Resilience4j                         | Circuit-breaker integration in `api-gateway` to help isolate failing downstream services.                                                                                                                                                                                                                |
| Caffeine                             | In-process cache implementation declared by several services and the gateway. This is a per-process cache, distinct from shared Redis state.                                                                                                                                                             |
| Docker / Docker Compose              | Each service has a Dockerfile. Compose files orchestrate the local platform and its supporting services, including PostgreSQL, Redis, RabbitMQ, and MailHog.                                                                                                                                             |

## Technologies by service

| Service                                       | Main technologies and their purpose                                                                                                                                                                                                   |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| API Gateway (`api-gateway`)                   | Spring Cloud Gateway Server WebMVC for routing; Spring Security and JJWT for bearer-token validation and role checks; Eureka and Config clients; Resilience4j circuit breakers; Caffeine cache dependency; Actuator health endpoints. |
| Service Registry (`service-registry`)         | Spring Cloud Netflix Eureka Server for service registration/discovery; Actuator for health checks.                                                                                                                                    |
| Configuration Server (`config-server`)        | Spring Cloud Config Server, with repository configuration files under `src/main/resources/config-repo`; Actuator health endpoint.                                                                                                     |
| Auth Service (`authservice`)                  | Spring MVC, Spring Security, JPA/PostgreSQL, Flyway, Bean Validation, Eureka/Config clients, Actuator, and JJWT for registration, login, token refresh, and account recovery.                                                         |
| Flight Service (`flight-service`)             | Spring MVC, JPA/PostgreSQL, Flyway, Bean Validation, caching, and security/JWT dependencies for airports, aircraft, flight schedules, search, and operational status.                                                                 |
| Pricing Service (`pricing-service`)           | Spring MVC and Bean Validation for fare quotes; Eureka/Config clients and Actuator. Its declared dependencies do not include a relational database; quote logic operates on quote request data.                                       |
| Passenger Service (`passenger-service`)       | Spring MVC, JPA/PostgreSQL, Bean Validation, caching, and Eureka/Config clients for passenger profiles and related traveller records.                                                                                                 |
| Booking Service (`booking-service`)           | Spring MVC, JPA/PostgreSQL, Bean Validation, Redis for seat locks, RabbitMQ for payment events, and Eureka/Config clients for reservations, PNRs, and check-in.                                                                       |
| Payment Service (`payment-service`)           | Spring MVC, JPA/PostgreSQL, Bean Validation, RabbitMQ publishing, and Eureka/Config clients for payment intents, webhooks, and payment lifecycle events.                                                                              |
| Notification Service (`notification-service`) | RabbitMQ consumer, Spring Mail, Thymeleaf email templates, and PostgreSQL/JPA delivery logs. It supports Mailgun and SMTP delivery; MailHog is included for local email testing.                                                      |
| Admin Service (`admin-service`)               | Spring MVC, JPA/PostgreSQL, caching, and Eureka/Config clients for protected operational endpoints and reporting support.                                                                                                             |

## Data and request flow

1. The frontend sends API requests to the API Gateway rather than calling
   business-service ports directly.
2. The gateway validates access tokens and routes requests to the appropriate
   service, using Eureka discovery and centrally supplied configuration.
3. Each data-owning service accesses its own relational data through JPA. A
   service must not read or write another service's database directly.
4. Booking coordination uses Redis for temporary seat locks. Payment and
   notification events are exchanged through RabbitMQ rather than requiring
   synchronous database sharing.
5. PostgreSQL is used in the supplied containerized environment. H2 is only
   available for selected development/test configurations.

## Where to verify or configure these technologies

- Dependency versions and service-specific libraries: each service's `pom.xml`.
- Shared/service configuration: `config-server/src/main/resources/config-repo/`.
- Local infrastructure and environment-variable wiring: `docker-compose.yml`
  and the other Compose variants.
- API paths and request/response contracts: [`openapi.yaml`](openapi.yaml).
- Service boundaries and data ownership rules: [`architecture.md`](architecture.md).

For production, set credentials and signing/webhook secrets through deployment
environment settings or a secret manager. Do not use the development
credentials shown in local Compose configuration.
