package com.switchwon.payment.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotBlank String customerId,
        @NotNull @Positive(message = "결제 금액은 0보다 커야 합니다") Long amount
) {
}