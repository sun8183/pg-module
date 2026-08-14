package com.switchwon.payment.wallet.service;

import com.switchwon.payment.common.exception.ApiException;
import com.switchwon.payment.common.response.ResponseCode;
import com.switchwon.payment.wallet.domain.CustomerWallet;
import com.switchwon.payment.wallet.repository.CustomerWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final CustomerWalletRepository walletRepository;

    public WalletService(CustomerWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public CustomerWallet charge(String customerId, Long amount) {
        walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> walletRepository.save(new CustomerWallet(customerId, 0L)));

        int updated = walletRepository.charge(customerId, amount);
        if (updated == 0) {
            throw new ApiException(ResponseCode.SYSTEM_ERROR, "지갑 충전에 실패했습니다.");
        }
        return walletRepository.findByCustomerId(customerId).orElseThrow();
    }

    @Transactional
    public boolean deductIfSufficient(String customerId, Long amount) {
        return walletRepository.deductIfSufficient(customerId, amount) == 1;
    }

    @Transactional(readOnly = true)
    public CustomerWallet getWallet(String customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ApiException(ResponseCode.NOT_FOUND, "고객 지갑을 찾을 수 없습니다."));
    }
}