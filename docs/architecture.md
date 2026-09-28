# Service Architecture

Each deployable service owns its data and exposes only its HTTP/event contracts. Services must not access another service's database.

## Internal module convention

New code and incremental refactors use these package roles:

| Module | Responsibility | May depend on |
| --- | --- | --- |
| `api` | Controllers, request validation, response mapping | `application` only |
| `application` | Use cases, transactions, orchestration | `domain`, `persistence`, `infrastructure` ports |
| `domain` | Entities, value objects, policies, business invariants | Java only |
| `persistence` | Repositories and database mappings | `domain` |
| `infrastructure` | Messaging, HTTP clients, security, configuration | application/domain ports |

The Pricing Service is the reference implementation of this pattern. Its controller only validates and delegates, `PricingQuoteService` performs the quote use case, and `PricingPolicy` contains pure pricing rules.

## Migration rules

1. Preserve public routes and event payloads while moving code.
2. Move a vertical feature at a time, with tests before and after each move.
3. Do not introduce shared persistence models or a shared database.
4. Cross-service calls belong behind an infrastructure client/port; controllers and domain code do not call remote services.
5. Business policy belongs in domain/application classes, never in controllers or message listeners.

## Service ownership

- Auth: identities, credentials, roles, refresh and recovery tokens.
- Flight: schedules, routes, aircraft, airports, capacity, operational status.
- Pricing: quote calculation only; it never changes a flight's base fare.
- Passenger: traveller profiles, documents, loyalty state.
- Booking: PNRs, reservation lifecycle and seat locks.
- Payment: payment and refund lifecycle, invoice views.
- Notification: delivery of domain events.
- Admin: operational actions and reporting composition.
