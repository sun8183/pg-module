package com.switchwon.payment.payment.repository;

import com.switchwon.payment.payment.domain.PaymentLedger;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, Long> {

    Optional<PaymentLedger> findByIdempotencyKey(String idempotencyKey);

    Page<PaymentLedger> findByCustomerId(String customerId, Pageable pageable);
}
