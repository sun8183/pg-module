package com.switchwon.payment.payment.controller;

import com.switchwon.payment.common.response.ApiResponse;
import com.switchwon.payment.payment.dto.PaymentRequest;
import com.switchwon.payment.payment.dto.PaymentResponse;
import com.switchwon.payment.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<PaymentResponse> pay(
            @RequestHeader("Idempotency-Key") @Size(min = 2, max = 10, message = "Idempotency-Key는 2~10자여야 합니다") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success(paymentService.pay(request.customerId(), request.amount(), idempotencyKey));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<Page<PaymentResponse>> getHistory(
            @PathVariable String customerId,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(paymentService.getHistory(customerId, pageable));
    }
}
