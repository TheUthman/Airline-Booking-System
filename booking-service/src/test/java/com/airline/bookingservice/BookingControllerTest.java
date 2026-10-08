package com.airline.bookingservice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

class BookingControllerTest {
    @Test
    void staffCanLookUpBookingWithoutExposingOwnerEmail() {
        BookingRepository repository = mock(BookingRepository.class);
        Booking booking = new Booking();
        booking.setPnr("ABC123");
        booking.setOwnerEmail("traveler@example.com");
        booking.setFlightId(12L);
        booking.setSeatNumber("12A");
        booking.setCabinClass("ECONOMY");
        booking.setAmount(BigDecimal.TEN);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        when(repository.findByPnrIgnoreCase("ABC123")).thenReturn(java.util.Optional.of(booking));
        BookingController controller =
                new BookingController(repository, mock(StringRedisTemplate.class));

        BookingController.StaffBookingView result = controller.staffLookup(" ABC123 ", "STAFF");

        assertThat(result.pnr()).isEqualTo("ABC123");
        assertThat(result.flightId()).isEqualTo(12L);
        assertThat(result.seatNumber()).isEqualTo("12A");
        assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void passengerCannotUseStaffBookingLookup() {
        BookingController controller =
                new BookingController(mock(BookingRepository.class), mock(StringRedisTemplate.class));

        assertThatThrownBy(() -> controller.staffLookup("ABC123", "PASSENGER"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Staff access is required");
    }

    @Test
    @SuppressWarnings("unchecked")
    void persistsTheSelectedCabinClass() {
        BookingRepository repository = mock(BookingRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        BookingController controller = new BookingController(repository, redis);

        controller.create(
                "traveler@example.com",
                new BookingController.BookingRequest(1L, 2L, "1A", BigDecimal.TEN, "business"));

        ArgumentCaptor<Booking> savedBooking = ArgumentCaptor.forClass(Booking.class);
        verify(repository).save(savedBooking.capture());
        assertThat(savedBooking.getValue().getCabinClass()).isEqualTo("BUSINESS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refusesAnAlreadyLockedSeat() {
        BookingRepository repository = mock(BookingRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(false);
        BookingController controller = new BookingController(repository, redis);

        assertThatThrownBy(
                        () ->
                                controller.create(
                                        "traveler@example.com",
                                        new BookingController.BookingRequest(
                                                1L, 2L, "12A", java.math.BigDecimal.TEN)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("currently being selected");
        verify(repository, never()).save(any());
    }
}
