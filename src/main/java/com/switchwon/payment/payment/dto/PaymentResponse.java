package com.switchwon.payment.payment.dto;

import com.switchwon.payment.payment.domain.PaymentLedger;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long ledgerId,
        String customerId,
        String type,
        Long amount,
        String status,
        String reason,
        String externalTxnId,
        LocalDateTime requestedAt,
        LocalDateTime completedAt
) {

    public static PaymentResponse from(PaymentLedger ledger) {
        return new PaymentResponse(
                ledger.getId(),
                ledger.getCustomerId(),
                ledger.getType().name(),
                ledger.getAmount(),
                ledger.getStatus().name(),
                ledger.getReason() == null ? null : ledger.getReason().name(),
                ledger.getExternalTxnId(),
                ledger.getRequestedAt(),
                ledger.getCompletedAt()
        );
    }
}