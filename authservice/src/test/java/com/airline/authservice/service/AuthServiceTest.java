package com.airline.authservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airline.authservice.dto.AuthResponse;
import com.airline.authservice.dto.RefreshTokenRequest;
import com.airline.authservice.entity.RefreshToken;
import com.airline.authservice.entity.Role;
import com.airline.authservice.entity.User;
import com.airline.authservice.repository.RefreshTokenRepository;
import com.airline.authservice.repository.UserRepository;
import com.airline.authservice.security.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, 604800000L);
        user = new User();
        user.setId(1L);
        user.setFirstName("Alex");
        user.setLastName("Taylor");
        user.setEmail("alex@example.com");
        user.setRole(Role.PASSENGER);
    }

    @Test
    void refreshTokenRevokesOldTokenAndReturnsReplacement() {
        String oldTokenValue = "old-refresh-token";
        String newTokenValue = "new-refresh-token";
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(oldTokenValue);
        storedToken.setUser(user);
        storedToken.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.of(storedToken));
        when(jwtService.isRefreshTokenValid(oldTokenValue)).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn(newTokenValue);
        when(jwtService.generateToken(user)).thenReturn("new-access-token");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(oldTokenValue);
        AuthResponse response = authService.refreshToken(request);

        assertTrue(storedToken.isRevoked());
        assertEquals(newTokenValue, response.getRefreshToken());
        assertNotEquals(oldTokenValue, response.getRefreshToken());
        assertEquals("new-access-token", response.getToken());

        ArgumentCaptor<RefreshToken> savedToken = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(savedToken.capture());
        assertEquals(newTokenValue, savedToken.getValue().getToken());
        assertEquals(user, savedToken.getValue().getUser());
        assertTrue(savedToken.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
    }
}
