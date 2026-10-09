package com.airline.passengerservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PassengerControllerTest {
    @Test
    void staffManifestReturnsOnlyPassengerNames() {
        PassengerRepository repository = mock(PassengerRepository.class);
        Passenger passenger = mock(Passenger.class);
        when(passenger.getId()).thenReturn(5L);
        when(passenger.getFirstName()).thenReturn("Ada");
        when(passenger.getLastName()).thenReturn("Lovelace");
        when(repository.findAllById(List.of(5L))).thenReturn(List.of(passenger));
        PassengerController controller = new PassengerController(repository);

        var result = controller.staffManifestPassengers("STAFF", List.of(5L));

        assertThat(result).containsExactly(
                new PassengerController.StaffManifestPassengerView(
                        5L, "Ada", "Lovelace"));
        verify(repository).findAllById(List.of(5L));
    }

    @Test
    void passengerCannotUseStaffManifestLookup() {
        PassengerRepository repository = mock(PassengerRepository.class);
        PassengerController controller = new PassengerController(repository);

        assertThatThrownBy(() -> controller.staffManifestPassengers("PASSENGER", List.of(5L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Staff access is required");
        verifyNoInteractions(repository);
    }
}
