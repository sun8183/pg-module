package com.switchwon.payment.payment.service;

import com.switchwon.payment.common.exception.ApiException;
import com.switchwon.payment.common.response.ResponseCode;
import com.switchwon.payment.payment.domain.FailureReason;
import com.switchwon.payment.payment.domain.LedgerType;
import com.switchwon.payment.payment.domain.PaymentLedger;
import com.switchwon.payment.payment.repository.PaymentLedgerRepository;
import com.switchwon.payment.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentLedgerCommandService {

    private final PaymentLedgerRepository ledgerRepository;
    private final WalletService walletService;

    public PaymentLedgerCommandService(PaymentLedgerRepository ledgerRepository, WalletService walletService) {
        this.ledgerRepository = ledgerRepository;
        this.walletService = walletService;
    }

    @Transactional
    public PaymentLedger savePending(String customerId, LedgerType type, Long amount, String idempotencyKey) {
        return ledgerRepository.save(PaymentLedger.pending(customerId, type, amount, idempotencyKey));
    }

    @Transactional
    public PaymentLedger completePayment(Long ledgerId, String customerId, Long amount, String externalTxnId) {
        boolean deducted = walletService.deductIfSufficient(customerId, amount);
        PaymentLedger ledger = getLedger(ledgerId);
        if (deducted) {
            ledger.complete(externalTxnId);
        } else {
            ledger.fail(FailureReason.INSUFFICIENT_BALANCE);
        }
        return ledger;
    }

    @Transactional
    public PaymentLedger completeCharge(Long ledgerId, String customerId, Long amount, String externalTxnId) {
        walletService.charge(customerId, amount);
        PaymentLedger ledger = getLedger(ledgerId);
        ledger.complete(externalTxnId);
        return ledger;
    }

    @Transactional
    public PaymentLedger markSystemError(Long ledgerId) {
        PaymentLedger ledger = getLedger(ledgerId);
        ledger.fail(FailureReason.SYSTEM_ERROR);
        return ledger;
    }

    private PaymentLedger getLedger(Long ledgerId) {
        return ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new ApiException(ResponseCode.SYSTEM_ERROR, "결제 원장을 찾을 수 없습니다."));
    }
}