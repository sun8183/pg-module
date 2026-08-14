package com.switchwon.payment.payment.gateway;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FakePaymentGatewayClient implements PaymentGatewayClient {

    private final double failureRate;
    private final long latencyMillis;

    public FakePaymentGatewayClient(double failureRate, long latencyMillis) {
        this.failureRate = failureRate;
        this.latencyMillis = latencyMillis;
    }

    @Override
    public ApprovalResult approve(String customerId, Long amount) {
        simulateLatency();
        if (failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new PaymentGatewayException("PG 승인 처리 중 오류가 발생했습니다.");
        }
        return new ApprovalResult("PG-" + UUID.randomUUID());
    }

    private void simulateLatency() {
        if (latencyMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("PG 승인 처리가 중단되었습니다.");
        }
    }
}