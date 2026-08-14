package com.switchwon.payment.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_ledger")
public class PaymentLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerType type;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerStatus status;

    @Enumerated(EnumType.STRING)
    private FailureReason reason;

    @Column(name = "external_txn_id")
    private String externalTxnId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected PaymentLedger() {
    }

    private PaymentLedger(String customerId, LedgerType type, Long amount, String idempotencyKey) {
        this.customerId = customerId;
        this.type = type;
        this.amount = amount;
        this.status = LedgerStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
    }

    public static PaymentLedger pending(String customerId, LedgerType type, Long amount, String idempotencyKey) {
        return new PaymentLedger(customerId, type, amount, idempotencyKey);
    }

    @PrePersist
    public void prePersist() {
        this.requestedAt = LocalDateTime.now();
    }

    public void complete(String externalTxnId) {
        this.status = LedgerStatus.COMPLETED;
        this.externalTxnId = externalTxnId;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(FailureReason reason) {
        this.status = LedgerStatus.FAILED;
        this.reason = reason;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public LedgerType getType() {
        return type;
    }

    public Long getAmount() {
        return amount;
    }

    public LedgerStatus getStatus() {
        return status;
    }

    public FailureReason getReason() {
        return reason;
    }

    public String getExternalTxnId() {
        return externalTxnId;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}