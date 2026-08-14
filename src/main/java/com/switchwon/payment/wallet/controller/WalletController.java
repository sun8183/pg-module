package com.switchwon.payment.wallet.controller;

import com.switchwon.payment.common.response.ApiResponse;
import com.switchwon.payment.payment.dto.PaymentResponse;
import com.switchwon.payment.payment.service.PaymentService;
import com.switchwon.payment.wallet.dto.ChargeRequest;
import com.switchwon.payment.wallet.dto.WalletResponse;
import com.switchwon.payment.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private final PaymentService paymentService;

    public WalletController(WalletService walletService, PaymentService paymentService) {
        this.walletService = walletService;
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    public ApiResponse<PaymentResponse> charge(
            @RequestHeader("Idempotency-Key") @Size(min = 2, max = 10, message = "Idempotency-Key는 2~10자여야 합니다") String idempotencyKey,
            @Valid @RequestBody ChargeRequest request) {
        return ApiResponse.success(paymentService.charge(request.customerId(), request.amount(), idempotencyKey));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<WalletResponse> getWallet(@PathVariable String customerId) {
        return ApiResponse.success(WalletResponse.from(walletService.getWallet(customerId)));
    }
}