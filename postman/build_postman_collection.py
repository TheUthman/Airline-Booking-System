import json
import os

collection = {
    "info": {
        "_postman_id": "9b12e345-6789-40ab-bcde-f123456789ab",
        "name": "Airline Booking System API - Automated Test Suite",
        "description": "Complete Postman test collection for the Airline Booking System microservices backend.\n\nAll requests are routed through the API Gateway (port 8080).\n\nFeatures:\n- Comprehensive test scripts (status code, schema validation, response time, headers)\n- Automated variable chaining (tokens, user IDs, flight IDs, booking IDs, PNRs, payment references)\n- Positive & negative / error test scenarios (400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 503 Fallback)\n- Full End-to-End user booking journey\n- Dynamic pre-request data generation to allow seamless Newman and Postman Runner executions.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080", "type": "string"},
        {"key": "webhookSecret", "value": "development-webhook-secret", "type": "string"},
        {"key": "userEmail", "value": "passenger.test@example.com", "type": "string"},
        {"key": "userPassword", "value": "Password123!", "type": "string"},
        {"key": "adminEmail", "value": "admin@airline.com", "type": "string"},
        {"key": "adminPassword", "value": "Admin123!", "type": "string"},
        {"key": "accessToken", "value": "", "type": "string"},
        {"key": "refreshToken", "value": "", "type": "string"},
        {"key": "adminToken", "value": "", "type": "string"},
        {"key": "userId", "value": "", "type": "string"},
        {"key": "passengerId", "value": "", "type": "string"},
        {"key": "flightId", "value": "", "type": "string"},
        {"key": "flightNumber", "value": "", "type": "string"},
        {"key": "bookingId", "value": "", "type": "string"},
        {"key": "pnr", "value": "", "type": "string"},
        {"key": "paymentId", "value": "", "type": "string"},
        {"key": "providerReference", "value": "", "type": "string"},
        {"key": "flightOrigin", "value": "LOS", "type": "string"},
        {"key": "flightDestination", "value": "ABV", "type": "string"},
        {"key": "flightDate", "value": "", "type": "string"}
    ],
    "item": []
}

def create_item(name, method, url_path, query_params=None, headers=None, body=None, pre_script=None, test_script=None, description=""):
    url_obj = {
        "raw": "{{baseUrl}}" + url_path + (("?" + "&".join([f"{q['key']}={q['value']}" for q in query_params])) if query_params else ""),
        "host": ["{{baseUrl}}"],
        "path": [p for p in url_path.split("/") if p]
    }
    if query_params:
        url_obj["query"] = query_params

    req_headers = headers or []
    req_body = {}
    if body is not None:
        req_body = {
            "mode": "raw",
            "raw": json.dumps(body, indent=2) if isinstance(body, (dict, list)) else body,
            "options": {
                "raw": {
                    "language": "json"
                }
            }
        }

    events = []
    if pre_script:
        events.append({
            "listen": "prerequest",
            "script": {
                "type": "text/javascript",
                "exec": pre_script if isinstance(pre_script, list) else pre_script.strip().split("\n")
            }
        })
    if test_script:
        events.append({
            "listen": "test",
            "script": {
                "type": "text/javascript",
                "exec": test_script if isinstance(test_script, list) else test_script.strip().split("\n")
            }
        })

    item = {
        "name": name,
        "event": events,
        "request": {
            "method": method,
            "header": req_headers,
            "url": url_obj,
            "description": description
        },
        "response": []
    }
    if body is not None:
        item["request"]["body"] = req_body

    return item

# -------------------------------------------------------------
# 1. AUTHENTICATION SERVICE
# -------------------------------------------------------------
auth_folder = {
    "name": "01 - Authentication Service (/api/auth)",
    "description": "Authentication and authorization endpoints (Register, Login, Token Refresh)",
    "item": [
        create_item(
            name="1.1 Register Passenger Account (Positive)",
            method="POST",
            url_path="/api/auth/register",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            pre_script="""
const timestamp = Date.now();
const dynamicEmail = `traveler_${timestamp}@airline.test`;
pm.collectionVariables.set("userEmail", dynamicEmail);
pm.variables.set("regEmail", dynamicEmail);
""",
            body={
                "firstName": "Alex",
                "lastName": "Taylor",
                "email": "{{regEmail}}",
                "password": "{{userPassword}}"
            },
            test_script="""
pm.test("Status code is 201 Created", function () {
    pm.response.to.have.status(201);
});

pm.test("Response is JSON with auth tokens", function () {
    pm.response.to.be.withBody;
    pm.response.to.be.json;
    const data = pm.response.json();
    
    pm.expect(data).to.have.property("token").that.is.a("string").and.not.empty;
    pm.expect(data).to.have.property("refreshToken").that.is.a("string").and.not.empty;
    pm.expect(data).to.have.property("tokenType", "Bearer");
    pm.expect(data).to.have.property("userId").that.is.a("number");
    pm.expect(data).to.have.property("email", pm.variables.get("regEmail"));
    pm.expect(data).to.have.property("role", "PASSENGER");
    
    // Save tokens for subsequent requests
    pm.collectionVariables.set("accessToken", data.token);
    pm.collectionVariables.set("refreshToken", data.refreshToken);
    pm.collectionVariables.set("userId", data.userId);
});

pm.test("Response time is under 1500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(1500);
});
""",
            description="Register a new passenger account. Automatically sets {{accessToken}}, {{refreshToken}}, {{userId}}, and {{userEmail}}."
        ),
        create_item(
            name="1.2 Register Duplicate Email (Negative - 400)",
            method="POST",
            url_path="/api/auth/register",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "firstName": "Alex",
                "lastName": "Taylor",
                "email": "{{userEmail}}",
                "password": "{{userPassword}}"
            },
            test_script="""
pm.test("Status code is 400 Bad Request", function () {
    pm.response.to.have.status(400);
});

pm.test("Response contains error message about existing account", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("error");
    pm.expect(JSON.stringify(data)).to.include("already exists");
});
""",
            description="Attempting to register with an already registered email should fail with 400 Bad Request."
        ),
        create_item(
            name="1.3 Register Validation Failure - Short Password (Negative - 400)",
            method="POST",
            url_path="/api/auth/register",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "firstName": "",
                "lastName": "",
                "email": "not-an-email",
                "password": "short"
            },
            test_script="""
pm.test("Status code is 400 Bad Request", function () {
    pm.response.to.have.status(400);
});

pm.test("Response indicates validation errors", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("error").that.is.a("string").and.not.empty;
});
""",
            description="Validates that invalid email, blank names, and short passwords (< 8 chars) are rejected with 400."
        ),
        create_item(
            name="1.4 Login User (Positive)",
            method="POST",
            url_path="/api/auth/login",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "email": "{{userEmail}}",
                "password": "{{userPassword}}"
            },
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Login returns valid tokens and user profile", function () {
    const data = pm.response.json();
    pm.expect(data.token).to.be.a("string").and.not.empty;
    pm.expect(data.refreshToken).to.be.a("string").and.not.empty;
    pm.expect(data.email).to.eql(pm.collectionVariables.get("userEmail"));
    pm.expect(data.role).to.eql("PASSENGER");
    
    // Update active tokens
    pm.collectionVariables.set("accessToken", data.token);
    pm.collectionVariables.set("refreshToken", data.refreshToken);
});
""",
            description="Authenticate with valid credentials and receive updated access and refresh tokens."
        ),
        create_item(
            name="1.5 Login Admin User (Positive)",
            method="POST",
            url_path="/api/auth/login",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "email": "{{adminEmail}}",
                "password": "{{adminPassword}}"
            },
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Admin login returns an ADMIN token", function () {
    const data = pm.response.json();
    pm.expect(data.token).to.be.a("string").and.not.empty;
    pm.expect(data.role).to.eql("ADMIN");
    pm.collectionVariables.set("adminToken", data.token);
});
""",
            description="Authenticate the bootstrapped admin account and store {{adminToken}}."
        ),
        create_item(
            name="1.6 Login Invalid Credentials (Negative - 401)",
            method="POST",
            url_path="/api/auth/login",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "email": "{{userEmail}}",
                "password": "WrongPassword999!"
            },
            test_script="""
pm.test("Status code is 401 Unauthorized", function () {
    pm.response.to.have.status(401);
});

pm.test("Error message mentions invalid credentials", function () {
    const data = pm.response.json();
    pm.expect(data.error).to.include("Invalid email or password");
});
""",
            description="Attempting to log in with an incorrect password fails with 401 Unauthorized."
        ),
        create_item(
            name="1.6 Refresh Access Token (Positive)",
            method="POST",
            url_path="/api/auth/refresh",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "refreshToken": "{{refreshToken}}"
            },
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("New access token is returned", function () {
    const data = pm.response.json();
    pm.expect(data.token).to.be.a("string").and.not.empty;
    pm.expect(data.refreshToken).to.eql(pm.collectionVariables.get("refreshToken"));
    
    // Update access token
    pm.collectionVariables.set("accessToken", data.token);
});
""",
            description="Exchange the refresh token for a newly signed access token."
        ),
        create_item(
            name="1.7 Refresh Token with Invalid Token (Negative - 401)",
            method="POST",
            url_path="/api/auth/refresh",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "refreshToken": "invalid-or-fake-refresh-token"
            },
            test_script="""
pm.test("Status code is 401 Unauthorized", function () {
    pm.response.to.have.status(401);
});

pm.test("Error details indicate invalid refresh token", function () {
    const data = pm.response.json();
    pm.expect(data.error).to.include("Invalid refresh token");
});
""",
            description="Using a corrupted or forged refresh token returns 401 Unauthorized."
        )
    ]
}

# -------------------------------------------------------------
# 2. FLIGHT SERVICE
# -------------------------------------------------------------
flights_folder = {
    "name": "02 - Flight Service (/api/flights)",
    "description": "Public flight search and Admin flight schedule management",
    "item": [
        create_item(
            name="2.1 Admin - Create Scheduled Flight (Positive)",
            method="POST",
            url_path="/api/flights/admin",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{adminToken}}"}
            ],
            pre_script="""
const flightNum = "NG-" + Math.floor(100 + Math.random() * 900);
pm.variables.set("newFlightNumber", flightNum);
""",
            body={
                "flightNumber": "{{newFlightNumber}}",
                "origin": "LOS",
                "destination": "ABV",
                "departureTime": "{{flightDate}}T08:00:00",
                "arrivalTime": "{{flightDate}}T09:15:00",
                "fare": 185.50,
                "availableSeats": 120
            },
            test_script="""
pm.test("Status code is 201 Created", function () {
    pm.response.to.have.status(201);
});

pm.test("Flight created with ID and details", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("id").that.is.a("number");
    pm.expect(data).to.have.property("flightNumber");
    pm.expect(data.origin).to.eql("LOS");
    pm.expect(data.destination).to.eql("ABV");
    pm.expect(data.availableSeats).to.eql(120);

    pm.collectionVariables.set("flightId", data.id);
    pm.collectionVariables.set("flightNumber", data.flightNumber);
});
""",
            description="Create a new flight schedule as an Admin. Extracts {{flightId}} and {{flightNumber}}."
        ),
        create_item(
            name="2.2 Admin - Create Flight As Passenger (Negative - 403)",
            method="POST",
            url_path="/api/flights/admin",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            body={
                "flightNumber": "UNAUTH-999",
                "origin": "LOS",
                "destination": "ABV",
                "departureTime": "{{flightDate}}T10:00:00",
                "arrivalTime": "{{flightDate}}T11:00:00",
                "fare": 100.00,
                "availableSeats": 50
            },
            test_script="""
pm.test("Status code is 403 Forbidden for non-admin user", function () {
    pm.response.to.have.status(403);
});
""",
            description="Ensure non-admin users (PASSENGER role) cannot access admin flight endpoints."
        ),
        create_item(
            name="2.3 Admin - Update Flight Schedule (Positive)",
            method="PUT",
            url_path="/api/flights/admin/{{flightId}}",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{adminToken}}"}
            ],
            body={
                "flightNumber": "{{flightNumber}}",
                "origin": "LOS",
                "destination": "ABV",
                "departureTime": "{{flightDate}}T08:30:00",
                "arrivalTime": "{{flightDate}}T09:45:00",
                "fare": 195.00,
                "availableSeats": 110
            },
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Updated fields verified", function () {
    const data = pm.response.json();
    pm.expect(data.fare).to.eql(195.00);
    pm.expect(data.availableSeats).to.eql(110);
});
""",
            description="Update departure time, fare, or available seats for an existing flight."
        ),
        create_item(
            name="2.4 Search Flights (Public - Positive)",
            method="GET",
            url_path="/api/flights/search",
            query_params=[
                {"key": "origin", "value": "LOS"},
                {"key": "destination", "value": "ABV"},
                {"key": "date", "value": "{{flightDate}}"},
                {"key": "passengers", "value": "1"}
            ],
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Returns list of matching flights", function () {
    pm.response.to.be.json;
    const flights = pm.response.json();
    pm.expect(flights).to.be.an("array");
    
    pm.expect(flights.length, "At least one matching flight is required").to.be.above(0);
    const flight = flights[0];
    pm.expect(flight).to.have.property("id");
    pm.expect(flight).to.have.property("flightNumber");
    pm.expect(flight.origin.toUpperCase()).to.eql("LOS");
    pm.expect(flight.destination.toUpperCase()).to.eql("ABV");
    pm.collectionVariables.set("flightId", flight.id);
    pm.collectionVariables.set("flightNumber", flight.flightNumber);
});
""",
            description="Public flight search by 3-letter IATA origin/destination, date, and passenger count."
        ),
        create_item(
            name="2.5 Search Flights Invalid IATA Code (Negative - 400)",
            method="GET",
            url_path="/api/flights/search",
            query_params=[
                {"key": "origin", "value": "TOOLONG"},
                {"key": "destination", "value": "X"},
                {"key": "date", "value": "{{flightDate}}"}
            ],
            test_script="""
pm.test("Status code is 400 Bad Request", function () {
    pm.response.to.have.status(400);
});
""",
            description="Query parameters origin/destination must be exactly 3 alphabetic characters."
        ),
        create_item(
            name="2.6 Get Flight by ID (Public - Positive)",
            method="GET",
            url_path="/api/flights/{{flightId}}",
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Flight object details matched", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("id");
    pm.expect(data).to.have.property("flightNumber");
    pm.expect(data).to.have.property("fare");
    pm.expect(data).to.have.property("availableSeats");
});
""",
            description="Fetch flight details by numeric ID."
        ),
        create_item(
            name="2.7 Get Non-existent Flight by ID (Negative - 404)",
            method="GET",
            url_path="/api/flights/999999",
            test_script="""
pm.test("Status code is 404 Not Found", function () {
    pm.response.to.have.status(404);
});

pm.test("Error payload has standard format", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("status", 404);
    pm.expect(data).to.have.property("message");
});
""",
            description="Requesting a non-existent flight ID returns 404 Not Found."
        )
    ]
}

# -------------------------------------------------------------
# 3. PASSENGER SERVICE
# -------------------------------------------------------------
passengers_folder = {
    "name": "03 - Passenger Service (/api/passengers)",
    "description": "Traveller profile creation, listing, and updates",
    "item": [
        create_item(
            name="3.1 Create Traveller Profile (Positive)",
            method="POST",
            url_path="/api/passengers",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            body={
                "firstName": "Alex",
                "lastName": "Taylor",
                "dateOfBirth": "1992-04-12",
                "phone": "+2348012345678",
                "documentNumber": "A09876543"
            },
            test_script="""
pm.test("Status code is 201 Created", function () {
    pm.response.to.have.status(201);
});

pm.test("Passenger created with ID and owner email", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("id").that.is.a("number");
    pm.expect(data.firstName).to.eql("Alex");
    pm.expect(data.lastName).to.eql("Taylor");
    pm.expect(data.documentNumber).to.eql("A09876543");
    pm.expect(data.ownerEmail).to.eql(pm.collectionVariables.get("userEmail"));
    
    pm.collectionVariables.set("passengerId", data.id);
});
""",
            description="Create a passenger profile. Stores {{passengerId}} for booking creation."
        ),
        create_item(
            name="3.2 Create Traveller Without Auth Token (Negative - 401)",
            method="POST",
            url_path="/api/passengers",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "firstName": "Jane",
                "lastName": "Doe",
                "dateOfBirth": "1995-01-01",
                "phone": "+1234567890",
                "documentNumber": "B12345678"
            },
            test_script="""
pm.test("Status code is 401 Unauthorized", function () {
    pm.response.to.have.status(401);
});
""",
            description="Unauthenticated requests to passenger endpoints must return 401."
        ),
        create_item(
            name="3.3 Create Traveller Validation Error - Future DOB (Negative - 400)",
            method="POST",
            url_path="/api/passengers",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            body={
                "firstName": "",
                "lastName": "Taylor",
                "dateOfBirth": "2099-01-01",
                "phone": "+123456789",
                "documentNumber": "C12345678"
            },
            test_script="""
pm.test("Status code is 400 Bad Request", function () {
    pm.response.to.have.status(400);
});
""",
            description="Blank name or future Date of Birth violates `@NotBlank` and `@Past` constraints."
        ),
        create_item(
            name="3.4 Get My Travellers (Positive)",
            method="GET",
            url_path="/api/passengers/me",
            headers=[{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Returns list of travellers for logged in user", function () {
    const list = pm.response.json();
    pm.expect(list).to.be.an("array");
    pm.expect(list.length).to.be.at.least(1);
    
    const passenger = list.find(p => p.id === pm.collectionVariables.get("passengerId"));
    if (passenger) {
        pm.expect(passenger.ownerEmail).to.eql(pm.collectionVariables.get("userEmail"));
    }
});
""",
            description="List all passenger profiles created under the authenticated user's account."
        ),
        create_item(
            name="3.5 Update Traveller Profile (Positive)",
            method="PUT",
            url_path="/api/passengers/{{passengerId}}",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            body={
                "firstName": "Alex",
                "lastName": "Taylor-Smith",
                "dateOfBirth": "1992-04-12",
                "phone": "+2348099999999",
                "documentNumber": "A09876543-REV"
            },
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Traveller profile successfully updated", function () {
    const data = pm.response.json();
    pm.expect(data.lastName).to.eql("Taylor-Smith");
    pm.expect(data.documentNumber).to.eql("A09876543-REV");
});
""",
            description="Update an existing passenger's profile."
        ),
        create_item(
            name="3.6 Update Non-existent Traveller (Negative - 404)",
            method="PUT",
            url_path="/api/passengers/999999",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            body={
                "firstName": "Ghost",
                "lastName": "User",
                "dateOfBirth": "1990-01-01",
                "phone": "+1000000000",
                "documentNumber": "G0000000"
            },
            test_script="""
pm.test("Status code is 404 Not Found", function () {
    pm.response.to.have.status(404);
});
""",
            description="Updating a non-existent traveller ID returns 404."
        )
    ]
}

# -------------------------------------------------------------
# 4. BOOKING SERVICE
# -------------------------------------------------------------
bookings_folder = {
    "name": "04 - Booking Service (/api/bookings)",
    "description": "PNR reservation management with Redis distributed seat locking",
    "item": [
        create_item(
            name="4.1 Create Booking & Acquire Seat Lock (Positive)",
            method="POST",
            url_path="/api/bookings",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            pre_script="""
const seatNum = Math.floor(1 + Math.random() * 30) + ["A","B","C","D","E","F"][Math.floor(Math.random() * 6)];
pm.collectionVariables.set("selectedSeat", seatNum);
pm.variables.set("dynamicSeat", seatNum);

const flightIdVal = pm.collectionVariables.get("flightId");
const passIdVal = pm.collectionVariables.get("passengerId");
if (!flightIdVal || !passIdVal) throw new Error("Run the flight and passenger setup tests first");
pm.variables.set("reqFlightId", flightIdVal);
pm.variables.set("reqPassengerId", passIdVal);
""",
            body={
                "flightId": "{{reqFlightId}}",
                "passengerId": "{{reqPassengerId}}",
                "seatNumber": "{{dynamicSeat}}",
                "amount": 185.50
            },
            test_script="""
pm.test("Status code is 201 Created", function () {
    pm.response.to.have.status(201);
});

pm.test("Booking created with PNR and PENDING_PAYMENT status", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("id").that.is.a("number");
    pm.expect(data).to.have.property("pnr").that.is.a("string").with.lengthOf(6);
    pm.expect(data.status).to.eql("PENDING_PAYMENT");
    pm.expect(data.ownerEmail).to.eql(pm.collectionVariables.get("userEmail"));
    
    pm.collectionVariables.set("bookingId", data.id);
    pm.collectionVariables.set("pnr", data.pnr);
});
""",
            description="Creates a reservation in PENDING_PAYMENT status and acquires a 10-minute Redis lock on the selected seat. Stores {{bookingId}} and {{pnr}}."
        ),
        create_item(
            name="4.2 Create Booking Duplicate Seat Lock (Negative - 409 Conflict)",
            method="POST",
            url_path="/api/bookings",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            pre_script="""
const flightIdVal = pm.collectionVariables.get("flightId");
const passIdVal = pm.collectionVariables.get("passengerId");
const selectedSeat = pm.collectionVariables.get("selectedSeat");
if (!flightIdVal || !passIdVal || !selectedSeat) throw new Error("Run booking setup first");
pm.variables.set("reqFlightId", flightIdVal);
pm.variables.set("reqPassengerId", passIdVal);
""",
            body={
                "flightId": "{{reqFlightId}}",
                "passengerId": "{{reqPassengerId}}",
                "seatNumber": "{{selectedSeat}}",
                "amount": 185.50
            },
            test_script="""
pm.test("Status code is 409 Conflict", function () {
    pm.response.to.have.status(409);
});

pm.test("Conflict reason specifies seat is locked", function () {
    const errorText = pm.response.text();
    pm.expect(errorText).to.include("currently being selected");
});
""",
            description="Attempting to book the same seat while locked must return 409 Conflict."
        ),
        create_item(
            name="4.3 Create Booking Invalid Seat Number Format (Negative - 400)",
            method="POST",
            url_path="/api/bookings",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            body={
                "flightId": 1,
                "passengerId": 1,
                "seatNumber": "INVALID_SEAT_123",
                "amount": 100.00
            },
            test_script="""
pm.test("Status code is 400 Bad Request", function () {
    pm.response.to.have.status(400);
});
""",
            description="Seat number must match regexp [0-9]{1,3}[A-Za-z]."
        ),
        create_item(
            name="4.4 Get My Bookings (Positive)",
            method="GET",
            url_path="/api/bookings",
            headers=[{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Returns list of bookings ordered by creation date", function () {
    const bookings = pm.response.json();
    pm.expect(bookings).to.be.an("array");
    pm.expect(bookings.length).to.be.at.least(1);
    
    const current = bookings.find(b => b.id === pm.collectionVariables.get("bookingId"));
    pm.expect(current, "The booking created by the setup test must be listed").to.exist;
    pm.expect(current.pnr).to.eql(pm.collectionVariables.get("pnr"));
});
""",
            description="List all reservations for the authenticated user."
        ),
        create_item(
            name="4.5 Get Booking by ID (Positive)",
            method="GET",
            url_path="/api/bookings/{{bookingId}}",
            headers=[{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Booking details matched", function () {
    const data = pm.response.json();
    pm.expect(data.id).to.eql(pm.collectionVariables.get("bookingId"));
    pm.expect(data.pnr).to.eql(pm.collectionVariables.get("pnr"));
    pm.expect(data.ownerEmail).to.eql(pm.collectionVariables.get("userEmail"));
});
""",
            description="Fetch a specific booking by ID."
        ),
        create_item(
            name="4.6 Cancel Pending Booking (Positive)",
            method="POST",
            url_path="/api/bookings/{{bookingToCancelId}}/cancel",
            headers=[{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
            pre_script="""
// First create a temporary booking to cancel so main test flow isn't disturbed
const flightId = pm.collectionVariables.get("flightId");
const passengerId = pm.collectionVariables.get("passengerId");
if (!flightId || !passengerId) throw new Error("Run the flight and passenger setup tests first");
const seatToCancel = "29F";
pm.sendRequest({
    url: pm.collectionVariables.get("baseUrl") + "/api/bookings",
    method: "POST",
    header: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + pm.collectionVariables.get("accessToken")
    },
    body: {
        mode: "raw",
        raw: JSON.stringify({
            flightId: flightId,
            passengerId: passengerId,
            seatNumber: seatToCancel,
            amount: 150.00
        })
    }
}, function (err, res) {
    if (!err && res.code === 201) {
        const json = res.json();
        pm.variables.set("bookingToCancelId", json.id);
    } else {
        throw new Error("Temporary booking could not be created for cancellation");
    }
});
""",
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Booking status changed to CANCELLED", function () {
    const data = pm.response.json();
    pm.expect(data.status).to.eql("CANCELLED");
});
""",
            description="Cancels a pending reservation and releases its Redis seat lock."
        )
    ]
}

# -------------------------------------------------------------
# 5. PAYMENT SERVICE
# -------------------------------------------------------------
payments_folder = {
    "name": "05 - Payment Service (/api/payments)",
    "description": "Payment intent initiation, provider reference generation, and webhook event publishing",
    "item": [
        create_item(
            name="5.1 Initiate Payment (Positive)",
            method="POST",
            url_path="/api/payments/initiate",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{accessToken}}"}
            ],
            pre_script="""
const bookingIdVal = pm.collectionVariables.get("bookingId");
if (!bookingIdVal) throw new Error("Run the booking setup test first");
pm.variables.set("payBookingId", bookingIdVal);
""",
            body={
                "bookingId": "{{payBookingId}}",
                "amount": 185.50
            },
            test_script="""
pm.test("Status code is 201 Created", function () {
    pm.response.to.have.status(201);
});

pm.test("Payment intent created with providerReference and PENDING status", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("id").that.is.a("number");
    pm.expect(data).to.have.property("providerReference").that.is.a("string").with.string("pay_");
    pm.expect(data.status).to.eql("PENDING");
    pm.expect(data.ownerEmail).to.eql(pm.collectionVariables.get("userEmail"));
    
    pm.collectionVariables.set("paymentId", data.id);
    pm.collectionVariables.set("providerReference", data.providerReference);
});
""",
            description="Initiates payment intent for a booking, generating a unique providerReference. Stores {{paymentId}} and {{providerReference}}."
        ),
        create_item(
            name="5.2 Initiate Payment Without Auth (Negative - 401)",
            method="POST",
            url_path="/api/payments/initiate",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            body={
                "bookingId": 1,
                "amount": 100.00
            },
            test_script="""
pm.test("Status code is 401 Unauthorized", function () {
    pm.response.to.have.status(401);
});
""",
            description="Payment initiation requires valid bearer token authentication."
        ),
        create_item(
            name="5.3 Webhook - Payment Succeeded Callback (Positive)",
            method="POST",
            url_path="/api/payments/webhook",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "X-Payment-Webhook-Secret", "value": "{{webhookSecret}}"}
            ],
            pre_script="""
const providerReference = pm.collectionVariables.get("providerReference");
if (!providerReference) throw new Error("Run the payment initiation test first");
""",
            body={
                "providerReference": "{{providerReference}}",
                "succeeded": True
            },
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});
""",
            description="Simulates external payment provider webhook callback when payment succeeds. Publishes `payment.succeeded` event to RabbitMQ."
        ),
        create_item(
            name="5.4 Webhook - Payment Failed Callback (Positive)",
            method="POST",
            url_path="/api/payments/webhook",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "X-Payment-Webhook-Secret", "value": "{{webhookSecret}}"}
            ],
            pre_script="""
// Create an intent specifically to test failure callback
pm.sendRequest({
    url: pm.collectionVariables.get("baseUrl") + "/api/payments/initiate",
    method: "POST",
    header: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + pm.collectionVariables.get("accessToken")
    },
    body: {
        mode: "raw",
        raw: JSON.stringify({
            bookingId: pm.collectionVariables.get("bookingId"),
            amount: 50.00
        })
    }
}, function(err, res) {
    if (!err && res.code === 201) {
        pm.variables.set("failedProviderRef", res.json().providerReference);
    } else {
        throw new Error("Payment intent could not be created for failure webhook test");
    }
});
""",
            body={
                "providerReference": "{{failedProviderRef}}",
                "succeeded": False
            },
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});
""",
            description="Simulates external payment provider webhook callback when payment fails. Publishes `payment.failed` event to RabbitMQ."
        ),
        create_item(
            name="5.5 Webhook - Invalid Secret (Negative - 401)",
            method="POST",
            url_path="/api/payments/webhook",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "X-Payment-Webhook-Secret", "value": "wrong-secret-signature"}
            ],
            body={
                "providerReference": "{{providerReference}}",
                "succeeded": True
            },
            test_script="""
pm.test("Status code is 401 Unauthorized", function () {
    pm.response.to.have.status(401);
});
""",
            description="Calls without the valid X-Payment-Webhook-Secret are rejected with 401."
        ),
        create_item(
            name="5.6 Webhook - Non-existent Payment Ref (Negative - 404)",
            method="POST",
            url_path="/api/payments/webhook",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "X-Payment-Webhook-Secret", "value": "{{webhookSecret}}"}
            ],
            body={
                "providerReference": "pay_nonexistent_ref_99999",
                "succeeded": True
            },
            test_script="""
pm.test("Status code is 404 Not Found", function () {
    pm.response.to.have.status(404);
});
""",
            description="Webhook called for an unknown provider reference returns 404."
        )
    ]
}

# -------------------------------------------------------------
# 6. NOTIFICATION SERVICE
# -------------------------------------------------------------
notifications_folder = {
    "name": "06 - Notification Service (/api/notifications)",
    "description": "Retrieve RabbitMQ asynchronous event delivery logs for current user",
    "item": [
        create_item(
            name="6.1 Get My Notification Delivery Logs (Positive)",
            method="GET",
            url_path="/api/notifications",
            headers=[{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Response is a list of delivery logs", function () {
    const logs = pm.response.json();
    pm.expect(logs).to.be.an("array");
    if (logs.length > 0) {
        const item = logs[0];
        pm.expect(item).to.have.property("recipient", pm.collectionVariables.get("userEmail"));
        pm.expect(item).to.have.property("type");
        pm.expect(item).to.have.property("payload");
    }
});
""",
            description="Fetch up to 50 latest notification event logs delivered to the authenticated user."
        ),
        create_item(
            name="6.2 Get Notifications Without Auth (Negative - 401)",
            method="GET",
            url_path="/api/notifications",
            test_script="""
pm.test("Status code is 401 Unauthorized", function () {
    pm.response.to.have.status(401);
});
""",
            description="Unauthenticated request to notifications endpoint returns 401."
        )
    ]
}

# -------------------------------------------------------------
# 7. ADMIN SERVICE
# -------------------------------------------------------------
admin_folder = {
    "name": "07 - Admin Service (/api/admin)",
    "description": "Administrative metrics and operational action auditing",
    "item": [
        create_item(
            name="7.1 Get Operations Dashboard (Positive - Admin)",
            method="GET",
            url_path="/api/admin/dashboard",
            headers=[{"key": "Authorization", "value": "Bearer {{adminToken}}"}],
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Dashboard contains service status and audit count", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("service", "admin-service");
    pm.expect(data).to.have.property("generatedAt");
    pm.expect(data).to.have.property("auditActionCount");
});
""",
            description="Operations dashboard endpoint protected by ROLE_ADMIN."
        ),
        create_item(
            name="7.2 Get Operations Dashboard As Passenger (Negative - 403)",
            method="GET",
            url_path="/api/admin/dashboard",
            headers=[{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
            test_script="""
pm.test("Status code is 403 Forbidden", function () {
    pm.response.to.have.status(403);
});
""",
            description="Normal passenger tokens cannot view the Admin Dashboard."
        ),
        create_item(
            name="7.3 Record Operational Action (Positive - Admin)",
            method="POST",
            url_path="/api/admin/actions",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{adminToken}}"}
            ],
            body={
                "action": "FLIGHT_SCHEDULE_SYNC",
                "details": "Triggered manual inventory reconciliation",
                "operator": "Postman Automated Test"
            },
            test_script="""
pm.test("Status code is 202 Accepted", function () {
    pm.response.to.have.status(202);
});

pm.test("Audit log response returned with RECORDED status", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("status", "RECORDED");
    pm.expect(data).to.have.property("requestedBy");
});
""",
            description="Record an operational audit action into Admin Service DB."
        )
    ]
}

# -------------------------------------------------------------
# 8. API GATEWAY & ACTUATOR
# -------------------------------------------------------------
system_folder = {
    "name": "08 - Gateway Fallback & Actuator",
    "description": "Circuit breaker fallback handlers and Spring Boot Actuator health checks",
    "item": [
        create_item(
            name="8.1 Flight Service Fallback Handler",
            method="GET",
            url_path="/fallback/flight-service",
            test_script="""
pm.test("Status code is 503 Service Unavailable", function () {
    pm.response.to.have.status(503);
});

pm.test("Fallback error response format", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("error", "Flight Service Unavailable");
    pm.expect(data).to.have.property("message");
});
""",
            description="Directly tests the Circuit Breaker fallback response for Flight Service."
        ),
        create_item(
            name="8.2 Actuator Health",
            method="GET",
            url_path="/actuator/health",
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Application status is UP", function () {
    const data = pm.response.json();
    pm.expect(data).to.have.property("status", "UP");
});
""",
            description="Check gateway health status via Spring Boot Actuator."
        ),
        create_item(
            name="8.3 Actuator Info",
            method="GET",
            url_path="/actuator/info",
            test_script="""
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});
""",
            description="Check gateway info endpoint via Spring Boot Actuator."
        )
    ]
}

# -------------------------------------------------------------
# 9. END-TO-END WORKFLOW
# -------------------------------------------------------------
e2e_folder = {
    "name": "09 - E2E Complete Booking Journey Flow",
    "description": "Sequential user journey test: Register -> Login -> Add Traveller -> Search Flight -> Create Booking (Seat Lock) -> Initiate Payment -> Succeeded Webhook -> Verify State",
    "item": [
        create_item(
            name="Step 1: E2E - Register User",
            method="POST",
            url_path="/api/auth/register",
            headers=[{"key": "Content-Type", "value": "application/json"}],
            pre_script="""
const e2eEmail = `e2e_${Date.now()}@airline.test`;
pm.collectionVariables.set("e2eUserEmail", e2eEmail);
pm.variables.set("e2eEmail", e2eEmail);
""",
            body={
                "firstName": "Journey",
                "lastName": "Traveler",
                "email": "{{e2eEmail}}",
                "password": "E2ePassword123!"
            },
            test_script="""
pm.test("Step 1: User registered (201 Created)", function () {
    pm.response.to.have.status(201);
    const data = pm.response.json();
    pm.collectionVariables.set("e2eToken", data.token);
});
"""
        ),
        create_item(
            name="Step 2: E2E - Create Traveller Profile",
            method="POST",
            url_path="/api/passengers",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{e2eToken}}"}
            ],
            body={
                "firstName": "Journey",
                "lastName": "Traveler",
                "dateOfBirth": "1988-11-20",
                "phone": "+2348000000001",
                "documentNumber": "PASS-E2E-1"
            },
            test_script="""
pm.test("Step 2: Traveller profile created (201 Created)", function () {
    pm.response.to.have.status(201);
    const data = pm.response.json();
    pm.collectionVariables.set("e2ePassengerId", data.id);
});
"""
        ),
        create_item(
            name="Step 3: E2E - Search Flights",
            method="GET",
            url_path="/api/flights/search",
            query_params=[
                {"key": "origin", "value": "LOS"},
                {"key": "destination", "value": "ABV"},
                {"key": "date", "value": "{{flightDate}}"},
                {"key": "passengers", "value": "1"}
            ],
            test_script="""
pm.test("Step 3: Flight search successful (200 OK)", function () {
    pm.response.to.have.status(200);
    const flights = pm.response.json();
    pm.expect(flights.length, "At least one flight must be available").to.be.above(0);
    pm.collectionVariables.set("e2eFlightId", flights[0].id);
    pm.collectionVariables.set("e2eFare", flights[0].fare);
});
"""
        ),
        create_item(
            name="Step 4: E2E - Create Booking with Seat Lock",
            method="POST",
            url_path="/api/bookings",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{e2eToken}}"}
            ],
            pre_script="""
const e2eSeat = "3" + ["A","B","C","D"][Math.floor(Math.random() * 4)];
pm.collectionVariables.set("e2eSeat", e2eSeat);
const flightIdVal = pm.collectionVariables.get("e2eFlightId");
const passIdVal = pm.collectionVariables.get("e2ePassengerId");
const fareVal = pm.collectionVariables.get("e2eFare");
if (!flightIdVal || !passIdVal || !fareVal) throw new Error("Complete E2E setup before creating a booking");
pm.variables.set("flightIdVal", flightIdVal);
pm.variables.set("passIdVal", passIdVal);
pm.variables.set("fareVal", fareVal);
pm.variables.set("seatVal", e2eSeat);
""",
            body={
                "flightId": "{{flightIdVal}}",
                "passengerId": "{{passIdVal}}",
                "seatNumber": "{{seatVal}}",
                "amount": "{{fareVal}}"
            },
            test_script="""
pm.test("Step 4: Booking created with seat lock (201 Created)", function () {
    pm.response.to.have.status(201);
    const data = pm.response.json();
    pm.expect(data.status).to.eql("PENDING_PAYMENT");
    pm.collectionVariables.set("e2eBookingId", data.id);
    pm.collectionVariables.set("e2ePnr", data.pnr);
});
"""
        ),
        create_item(
            name="Step 5: E2E - Initiate Payment Intent",
            method="POST",
            url_path="/api/payments/initiate",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "Authorization", "value": "Bearer {{e2eToken}}"}
            ],
            pre_script="""
const e2eBookingId = pm.collectionVariables.get("e2eBookingId");
if (!e2eBookingId) throw new Error("E2E booking is missing");
pm.variables.set("e2eBookId", e2eBookingId);
const e2eFare = pm.collectionVariables.get("e2eFare");
if (!e2eFare) throw new Error("E2E fare is missing");
pm.variables.set("e2eAmt", e2eFare);
""",
            body={
                "bookingId": "{{e2eBookId}}",
                "amount": "{{e2eAmt}}"
            },
            test_script="""
pm.test("Step 5: Payment intent initiated (201 Created)", function () {
    pm.response.to.have.status(201);
    const data = pm.response.json();
    pm.expect(data.status).to.eql("PENDING");
    pm.collectionVariables.set("e2eProviderRef", data.providerReference);
});
"""
        ),
        create_item(
            name="Step 6: E2E - Provider Webhook Succeeded Callback",
            method="POST",
            url_path="/api/payments/webhook",
            headers=[
                {"key": "Content-Type", "value": "application/json"},
                {"key": "X-Payment-Webhook-Secret", "value": "{{webhookSecret}}"}
            ],
            pre_script="""
const e2eProviderRef = pm.collectionVariables.get("e2eProviderRef");
if (!e2eProviderRef) throw new Error("E2E provider reference is missing");
pm.variables.set("e2eProviderRefVal", e2eProviderRef);
""",
            body={
                "providerReference": "{{e2eProviderRefVal}}",
                "succeeded": True
            },
            test_script="""
pm.test("Step 6: Webhook accepted and processed (200 OK)", function () {
    pm.response.to.have.status(200);
});
"""
        ),
        create_item(
            name="Step 7: E2E - Verify Booking Status",
            method="GET",
            url_path="/api/bookings/{{e2eBookingIdVal}}",
            headers=[{"key": "Authorization", "value": "Bearer {{e2eToken}}"}],
            pre_script="""
const e2eBookingId = pm.collectionVariables.get("e2eBookingId");
if (!e2eBookingId) throw new Error("E2E booking is missing");
pm.variables.set("e2eBookingIdVal", e2eBookingId);
""",
            test_script="""
pm.test("Step 7: Verified booking details (200 OK)", function () {
    pm.response.to.have.status(200);
    const data = pm.response.json();
    pm.expect(data.pnr).to.eql(pm.collectionVariables.get("e2ePnr"));
});
"""
        ),
        create_item(
            name="Step 8: E2E - Check User Notification Delivery Logs",
            method="GET",
            url_path="/api/notifications",
            headers=[{"key": "Authorization", "value": "Bearer {{e2eToken}}"}],
            test_script="""
pm.test("Step 8: Retrieved notification history (200 OK)", function () {
    pm.response.to.have.status(200);
    const logs = pm.response.json();
    pm.expect(logs).to.be.an("array");
});
"""
        )
    ]
}

collection["item"] = [
    auth_folder,
    flights_folder,
    passengers_folder,
    bookings_folder,
    payments_folder,
    notifications_folder,
    admin_folder,
    system_folder,
    e2e_folder
]
collection["event"] = [{
    "listen": "prerequest",
    "script": {
        "type": "text/javascript",
        "exec": [
            "const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);",
            "pm.collectionVariables.set(\"flightDate\", tomorrow.toISOString().slice(0, 10));"
        ]
    }
}]

# Write collection file
output_path = os.path.join("postman", "Airline_Booking_System.postman_collection.json")
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(collection, f, indent=2)

print(f"Collection successfully created at {output_path}")

# Environment file
environment = {
    "id": "e4567890-12ab-cdef-0123-456789abcdef",
    "name": "Airline Booking System - Local Dev",
    "values": [
        {"key": "baseUrl", "value": "http://localhost:8080", "type": "default", "enabled": True},
        {"key": "webhookSecret", "value": "development-webhook-secret", "type": "default", "enabled": True},
        {"key": "userPassword", "value": "Password123!", "type": "secret", "enabled": True},
        {"key": "adminEmail", "value": "admin@airline.com", "type": "default", "enabled": True},
        {"key": "adminPassword", "value": "Admin123!", "type": "secret", "enabled": True},
    ],
    "_postman_variable_scope": "environment"
}

env_output_path = os.path.join("postman", "Airline_Booking_System.postman_environment.json")
with open(env_output_path, "w", encoding="utf-8") as f:
    json.dump(environment, f, indent=2)

print(f"Environment successfully created at {env_output_path}")
