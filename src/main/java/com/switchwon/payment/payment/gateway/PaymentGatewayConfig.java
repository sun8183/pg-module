package com.switchwon.payment.payment.gateway;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentGatewayConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService paymentGatewayExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public PaymentGatewayClient paymentGatewayClient(
            ExecutorService paymentGatewayExecutor,
            @Value("${payment.gateway.fake.failure-rate:0.0}") double failureRate,
            @Value("${payment.gateway.fake.latency-ms:0}") long latencyMillis,
            @Value("${payment.gateway.timeout-ms:3000}") long timeoutMillis) {
        PaymentGatewayClient fakeClient = new FakePaymentGatewayClient(failureRate, latencyMillis);
        return new TimeoutPaymentGatewayClient(fakeClient, paymentGatewayExecutor, timeoutMillis);
    }
}