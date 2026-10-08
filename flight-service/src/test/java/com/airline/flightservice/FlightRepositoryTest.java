package com.airline.flightservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:flight-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FlightRepositoryTest {
    @Autowired
    private FlightRepository flights;

    @Test
    void searchesUsingTheSelectedCabinFareAndDoesNotOfferUnpricedBusinessClass() {
        LocalDateTime departure = LocalDate.now().plusDays(10).atTime(8, 0);
        flights.save(flight("TA101", departure, "100.00", "500.00"));
        flights.save(flight("TA102", departure.plusHours(1), "120.00", "0.00"));

        LocalDateTime start = departure.toLocalDate().atStartOfDay();
        LocalDateTime end = departure.toLocalDate().plusDays(1).atStartOfDay();

        assertThat(flights.search("LOS", "ABV", start, end, 1, "ECONOMY",
                null, new BigDecimal("150.00"), null))
                .extracting(Flight::getFlightNumber)
                .containsExactlyInAnyOrder("TA101", "TA102");
        assertThat(flights.search("LOS", "ABV", start, end, 1, "BUSINESS",
                null, new BigDecimal("600.00"), null))
                .extracting(Flight::getFlightNumber)
                .containsExactly("TA101");
        assertThat(flights.search("LOS", "ABV", start, end, 1, "BUSINESS",
                null, new BigDecimal("400.00"), null))
                .isEmpty();
    }

    private Flight flight(
            String number, LocalDateTime departure, String economyFare, String businessFare) {
        Flight flight = new Flight();
        flight.setFlightNumber(number);
        flight.setOrigin("LOS");
        flight.setDestination("ABV");
        flight.setDepartureTime(departure);
        flight.setArrivalTime(departure.plusHours(1));
        flight.setFare(new BigDecimal(economyFare));
        flight.setBusinessFare(new BigDecimal(businessFare));
        flight.setAvailableSeats(100);
        flight.setTotalSeats(100);
        flight.setAirline("TigerAirlines");
        return flight;
    }
}
