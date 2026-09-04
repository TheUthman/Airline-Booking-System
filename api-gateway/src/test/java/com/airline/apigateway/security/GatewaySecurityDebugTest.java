package com.airline.apigateway.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityDebugTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Test
    void publicAuthLoginShouldNotReturn403() {
        int status = restClientBuilder
                .build()
                .post()
                .uri("http://localhost:" + port + "/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"email\":\"test@example.com\",\"password\":\"password\"}")
                .exchange((request, response) -> response.getStatusCode().value());

        System.out.println("LOGIN status=" + status);
        assertNotEquals(403, status);
    }

    @Test
    void publicFlightSearchShouldNotReturn403() {
        int status = restClientBuilder
                .build()
                .get()
                .uri(
                        "http://localhost:"
                                + port
                                + "/api/flights/search?origin=LHR&destination=JFK")
                .exchange((request, response) -> response.getStatusCode().value());

        System.out.println("FLIGHTS status=" + status);
        assertNotEquals(403, status);
    }

    @Test
    void actuatorHealthShouldNotReturn403() {
        int status = restClientBuilder
                .build()
                .get()
                .uri("http://localhost:" + port + "/actuator/health")
                .exchange((request, response) -> response.getStatusCode().value());

        System.out.println("ACTUATOR status=" + status);
        assertNotEquals(403, status);
    }

    @Test
    void optionsPreflightShouldNotReturn403() {
        int status = restClientBuilder
                .build()
                .options()
                .uri("http://localhost:" + port + "/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .exchange((request, response) -> response.getStatusCode().value());

        System.out.println("OPTIONS status=" + status);
        assertNotEquals(403, status);
    }
}
