# Tiger Airlines cost-based pricing update

This patch adds an initial cost-based fare estimate to the Spring Boot backend. It is a starting model for a project/demo, not a certified airline revenue-management system. All default costs are illustrative and must be calibrated before real-world use.

## What changed

### Flight Service
- `Aircraft` now has estimated fuel consumption (litres/hour), cruise speed (km/hour), operating cost/hour and maintenance cost/hour.
- `Airport` now has latitude/longitude and an estimated airport charge.
- Admin airport and aircraft create/update payloads accept the new fields.
- `CostBasedFareCalculator` calculates great-circle route distance, estimated time, cost components, expected passengers, suggested economy fare and suggested business fare.
- `POST /api/flights/admin/fare-estimate` previews the calculation.
- Flight create/update calculates fares automatically unless `manualFareOverride: true` is explicitly sent.
- The estimate configuration can be supplied through environment variables or `flight-service/src/main/resources/application.yaml`.
- `V2__add_cost_based_pricing_fields.sql` documents the database columns to add. The current service uses Hibernate `ddl-auto: update`; if production uses Flyway-managed schema only, review and run migrations as part of deployment.

### Pricing Service
- Quote calculation now retrieves the flight's authoritative fare, departure time and seat inventory from Flight Service.
- Client-supplied `baseFare`, `departureDate`, `availableSeats` and `totalSeats` are not used as pricing inputs.
- Set `FLIGHT_SERVICE_URL` to the address that Pricing Service can use to reach Flight Service. Local default is `http://localhost:8083`; in Docker it may be `http://flight-service:8083`.
- The existing quote route remains `POST /api/pricing/quote`.

## Configure aircraft and airports first

The estimate endpoint will reject missing coordinates or aircraft cost specifications instead of silently producing a made-up fare.

Example airport payload (Lagos):
```json
{
  "code": "LOS",
  "name": "Murtala Muhammed International Airport",
  "city": "Lagos",
  "country": "Nigeria",
  "latitude": 6.5774,
  "longitude": 3.3212,
  "estimatedAirportCharge": 10000
}
```

Example airport payload (Abuja):
```json
{
  "code": "ABV",
  "name": "Nnamdi Azikiwe International Airport",
  "city": "Abuja",
  "country": "Nigeria",
  "latitude": 9.0068,
  "longitude": 7.2631,
  "estimatedAirportCharge": 12000
}
```

Example aircraft payload (illustrative specifications only):
```json
{
  "code": "TA-100",
  "model": "Example narrow-body aircraft",
  "seatCapacity": 100,
  "fuelConsumptionLitresPerHour": 900,
  "cruiseSpeedKmh": 800,
  "operatingCostPerHour": 200000,
  "maintenanceCostPerHour": 50000
}
```

Use `POST /api/flights/admin/airports` and `POST /api/flights/admin/aircraft` to create records, or the corresponding `PUT` endpoints to update existing records. Do not copy illustrative aircraft costs into production without validating them.

## Preview a fare

Request:
```http
POST /api/flights/admin/fare-estimate
Content-Type: application/json
```

```json
{
  "origin": "LOS",
  "destination": "ABV",
  "aircraftCode": "TA-100"
}
```

The response includes distance, estimated hours, aircraft capacity, expected load factor/passengers, cost breakdown, estimated total operating cost, other revenue assumption, target margin and suggested economy/business fares.

## Create a flight with automatic fares

Keep the existing flight fields, but omit `fare` and `businessFare` (or leave them null) and set `manualFareOverride` to false. Example:
```json
{
  "flightNumber": "TA101",
  "origin": "LOS",
  "destination": "ABV",
  "departureTime": "2027-02-15T09:00:00",
  "arrivalTime": "2027-02-15T10:20:00",
  "availableSeats": 100,
  "totalSeats": 100,
  "airline": "Tiger Airlines",
  "aircraftCode": "TA-100",
  "manualFareOverride": false
}
```

The backend calculates and saves `fare` and `businessFare`. The `totalSeats` value in the request should match the selected aircraft's capacity; the next improvement should make aircraft capacity the sole source of truth for seat inventory.

Manual override is explicit and optional:
```json
{
  "fare": 70000,
  "businessFare": 210000,
  "manualFareOverride": true
}
```
Include these fields alongside the normal flight fields only when an authorized admin intentionally overrides the automatic fare. The existing security configuration must restrict these admin routes to authorized staff.

## Pricing assumptions

Configuration keys in `flight-service/src/main/resources/application.yaml`:

| Environment variable | Default | Meaning |
|---|---:|---|
| `FUEL_PRICE_PER_LITRE` | `950` | Estimated fuel cost per litre in NGN |
| `ROUTE_DISTANCE_FACTOR` | `1.12` | Adjustment from straight-line distance to estimated flown distance |
| `TAXI_AND_OVERHEAD_HOURS` | `0.50` | Time allowance added to cruise time |
| `CREW_COST_PER_FLIGHT` | `120000` | Estimated crew cost for a flight |
| `EXPECTED_LOAD_FACTOR` | `0.75` | Expected share of seats sold |
| `TARGET_PROFIT_MARGIN` | `0.10` | Target margin from 0 to less than 0.80 |
| `OTHER_REVENUE_PER_FLIGHT` | `0` | Expected non-ticket revenue for a flight |
| `BUSINESS_FARE_MULTIPLIER` | `3.0` | Initial business fare multiple |

The calculator estimates:
- flight hours = (route distance × route-distance factor ÷ aircraft cruise speed) + time overhead;
- fuel cost = flight hours × fuel consumption/hour × fuel price/litre;
- operating and maintenance costs = their configured aircraft hourly rates × estimated flight hours;
- total cost = fuel + operating + maintenance + crew + airport charges;
- required total revenue = total cost ÷ (1 − target margin);
- required ticket revenue = required total revenue − other revenue;
- expected passengers = aircraft seats × expected load factor;
- suggested economy fare = required ticket revenue ÷ expected passengers, rounded upward to the nearest ₦100.

The fare is an estimate. It does not automatically include taxes or mandatory government fees. Add a separate tax/fee calculation before presenting the final checkout total.

## Verify locally

From the repository root, run the tests/builds for both services. If you have Maven installed:
```bash
mvn -f flight-service/pom.xml test
mvn -f pricing-service/pom.xml test
```

If your Windows environment uses Maven Wrapper, run the wrapper available in the repository with `-f flight-service/pom.xml test` and `-f pricing-service/pom.xml test`.

Before deploying:
1. Update existing airport records with coordinates and charges.
2. Update existing aircraft records with fuel consumption, cruise speed and cost assumptions.
3. Set `FLIGHT_SERVICE_URL` correctly for the network where services run.
4. Verify admin authorization for all `/api/flights/admin/**` endpoints.
5. Test fare estimates and booking/payment flows in a non-production environment.
6. Calibrate assumptions with reliable operating data. The defaults are placeholders, not actual Tiger Airlines costs.
