package com.switchwon.payment.wallet.dto;

import com.switchwon.payment.wallet.domain.CustomerWallet;

public record WalletResponse(String customerId, Long balance) {

    public static WalletResponse from(CustomerWallet wallet) {
        return new WalletResponse(wallet.getCustomerId(), wallet.getBalance());
    }
}