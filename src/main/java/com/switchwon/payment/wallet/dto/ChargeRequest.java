package com.switchwon.payment.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChargeRequest(
        @NotBlank String customerId,
        @NotNull @Positive(message = "충전 금액은 0보다 커야 합니다") Long amount
) {
}