package com.airline.bookingservice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

class BookingControllerTest {
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
