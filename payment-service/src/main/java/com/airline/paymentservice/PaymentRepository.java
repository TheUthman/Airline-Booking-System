package com.airline.paymentservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByProviderReference(String providerReference);
    List<Payment> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
    Optional<Payment> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
