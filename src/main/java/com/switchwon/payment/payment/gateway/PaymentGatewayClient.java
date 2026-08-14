package com.switchwon.payment.payment.gateway;

public interface PaymentGatewayClient {

    ApprovalResult approve(String customerId, Long amount);
}