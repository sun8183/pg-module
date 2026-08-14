package com.switchwon.payment.payment.service;

import com.switchwon.payment.payment.domain.LedgerStatus;
import com.switchwon.payment.payment.domain.LedgerType;
import com.switchwon.payment.payment.domain.PaymentLedger;
import com.switchwon.payment.payment.dto.PaymentResponse;
import com.switchwon.payment.payment.gateway.ApprovalResult;
import com.switchwon.payment.payment.gateway.PaymentGatewayClient;
import com.switchwon.payment.payment.gateway.PaymentGatewayException;
import com.switchwon.payment.payment.repository.PaymentLedgerRepository;
import com.switchwon.payment.wallet.service.WalletService;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentLedgerCommandService commandService;
    private final PaymentGatewayClient gatewayClient;
    private final WalletService walletService;
    private final PaymentLedgerRepository ledgerRepository;

    public PaymentService(PaymentLedgerCommandService commandService,
                           PaymentGatewayClient gatewayClient,
                           WalletService walletService,
                           PaymentLedgerRepository ledgerRepository) {
        this.commandService = commandService;
        this.gatewayClient = gatewayClient;
        this.walletService = walletService;
        this.ledgerRepository = ledgerRepository;
    }

    public PaymentResponse pay(String customerId, Long amount, String idempotencyKey) {
        return findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> processPay(customerId, amount, idempotencyKey));
    }

    private PaymentResponse processPay(String customerId, Long amount, String idempotencyKey) {
        walletService.getWallet(customerId);
        PaymentLedger pending = savePendingIdempotently(customerId, LedgerType.PAYMENT, amount, idempotencyKey);
        if (pending.getStatus() != LedgerStatus.PENDING) {
            return PaymentResponse.from(pending);
        }
        try {
            ApprovalResult result = gatewayClient.approve(customerId, amount);
            PaymentLedger settled = commandService.completePayment(
                    pending.getId(), customerId, amount, result.externalTxnId());
            return PaymentResponse.from(settled);
        } catch (PaymentGatewayException e) {
            PaymentLedger failed = commandService.markSystemError(pending.getId());
            return PaymentResponse.from(failed);
        }
    }

    public PaymentResponse charge(String customerId, Long amount, String idempotencyKey) {
        return findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> processCharge(customerId, amount, idempotencyKey));
    }

    private PaymentResponse processCharge(String customerId, Long amount, String idempotencyKey) {
        PaymentLedger pending = savePendingIdempotently(customerId, LedgerType.CHARGE, amount, idempotencyKey);
        if (pending.getStatus() != LedgerStatus.PENDING) {
            return PaymentResponse.from(pending);
        }
        try {
            ApprovalResult result = gatewayClient.approve(customerId, amount);
            PaymentLedger settled = commandService.completeCharge(
                    pending.getId(), customerId, amount, result.externalTxnId());
            return PaymentResponse.from(settled);
        } catch (PaymentGatewayException e) {
            PaymentLedger failed = commandService.markSystemError(pending.getId());
            return PaymentResponse.from(failed);
        }
    }

    // 동시 요청 race 시 DB unique 제약이 최종 방어선 — 위반 시 먼저 커밋된 원장을 반환
    private PaymentLedger savePendingIdempotently(String customerId, LedgerType type, Long amount, String idempotencyKey) {
        try {
            return commandService.savePending(customerId, type, amount, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            return ledgerRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
    }

    private Optional<PaymentResponse> findByIdempotencyKey(String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey).map(PaymentResponse::from);
    }

    public Page<PaymentResponse> getHistory(String customerId, Pageable pageable) {
        return ledgerRepository.findByCustomerId(customerId, pageable)
                .map(PaymentResponse::from);
    }
}
