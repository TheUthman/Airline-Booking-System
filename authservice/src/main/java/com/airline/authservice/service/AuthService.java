package com.airline.authservice.service;

import com.airline.authservice.dto.AuthResponse;
import com.airline.authservice.dto.LoginRequest;
import com.airline.authservice.dto.RefreshTokenRequest;
import com.airline.authservice.dto.RegisterRequest;
import com.airline.authservice.entity.RefreshToken;
import com.airline.authservice.entity.Role;
import com.airline.authservice.entity.User;
import com.airline.authservice.repository.RefreshTokenRepository;
import com.airline.authservice.repository.UserRepository;
import com.airline.authservice.security.JwtService;

import com.airline.authservice.client.NotificationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationClient notificationClient;
    private final long refreshExpiration;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NotificationClient notificationClient,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationClient = notificationClient;
        this.refreshExpiration = refreshExpiration;
    }

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(Role.PASSENGER);

        User savedUser = userRepository.save(user);

        // Generate email verification token and trigger email dispatch
        String verificationToken = UUID.randomUUID().toString().replace("-", "");
        savedUser.setEmailVerificationToken(verificationToken);
        savedUser = userRepository.save(savedUser);
        notificationClient.sendEmailVerification(savedUser.getEmail(), savedUser.getFirstName() + " " + savedUser.getLastName(), verificationToken);

        String accessToken = jwtService.generateToken(savedUser);

        String refreshToken = jwtService.generateRefreshToken(savedUser);

        saveRefreshToken(savedUser, refreshToken);

        return new AuthResponse(
                accessToken,
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                refreshToken);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);

        String refreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshToken);

        return new AuthResponse(
                accessToken,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String tokenValue = request.getRefreshToken();

        RefreshToken storedToken = refreshTokenRepository
                .findByToken(tokenValue)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new BadCredentialsException("Refresh token has expired");
        }

        if (!jwtService.isRefreshTokenValid(tokenValue)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = storedToken.getUser();

        storedToken.setRevoked(true);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        saveRefreshToken(user, newRefreshToken);

        String newAccessToken = jwtService.generateToken(user);

        return new AuthResponse(
                newAccessToken,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                newRefreshToken);
    }

    private void saveRefreshToken(User user, String token) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(token);
        refreshToken.setUser(user);

        refreshToken.setExpiresAt(
                LocalDateTime.now().plus(java.time.Duration.ofMillis(refreshExpiration)));

        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
    }

    public User promoteToAdmin(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "User " + userId + " not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("User " + userId + " is already an ADMIN");
        }

        user.setRole(Role.ADMIN);

        return userRepository.save(user);
    }

    public java.util.List<java.util.Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(user -> java.util.Map.<String, Object>of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName(),
                        "role", user.getRole().name(),
                        "name", user.getFirstName() + " " + user.getLastName()))
                .toList();
    }

    /** Creates a one-time token. Delivery is delegated to notification-service by the caller. */
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new BadCredentialsException("No account exists for this email"));
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
        notificationClient.sendPasswordReset(user.getEmail(), fullName.trim(), token);
        return token;
    }

    public void resetPassword(String token, String password) {
        User user = userRepository.findByPasswordResetToken(token).orElseThrow(
                () -> new BadCredentialsException("Invalid password reset token"));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Password reset token has expired");
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }

    public String requestEmailVerification(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new BadCredentialsException("No account exists for this email"));
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setEmailVerificationToken(token);
        userRepository.save(user);

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
        notificationClient.sendEmailVerification(user.getEmail(), fullName.trim(), token);
        return token;
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token).orElseThrow(
                () -> new BadCredentialsException("Invalid verification token"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
    }
}
