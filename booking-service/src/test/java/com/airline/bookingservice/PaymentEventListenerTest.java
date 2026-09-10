package com.airline.bookingservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Optional;

class PaymentEventListenerTest {
    @Test
    void confirmsPendingBookingWhenPaymentSucceeds() throws Exception {
        BookingRepository repository = mock(BookingRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setFlightId(17L);
        booking.setSeatNumber("12A");
        booking.setAmount(BigDecimal.TEN);
        when(repository.findById(9L)).thenReturn(Optional.of(booking));

        new PaymentEventListener(repository, redis, new ObjectMapper())
                .handle("{\"type\":\"payment.succeeded\",\"bookingId\":9}");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(repository).save(booking);
        verify(redis).delete("seat-lock:17:12A");
    }

    @Test
    void dropsUnparseablePayloadWithoutTouchingTheBooking() {
        BookingRepository repository = mock(BookingRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);

        // Poison message: not valid JSON and starting with an unquoted token such as
        // "the...". Must be ACKed and dropped, never retried or thrown.
        new PaymentEventListener(repository, redis, new ObjectMapper())
                .handle("the payment provider returned a maintenance page");

        verifyNoInteractions(repository);
        verifyNoInteractions(redis);
    }

    @Test
    void dropsEmptyPayloadWithoutTouchingTheBooking() {
        BookingRepository repository = mock(BookingRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);

        // readTree("") returns null; must be dropped rather than NPE.
        new PaymentEventListener(repository, redis, new ObjectMapper()).handle("");

        verifyNoInteractions(repository);
        verifyNoInteractions(redis);
    }
}
