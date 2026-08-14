package com.switchwon.payment.payment.gateway;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutPaymentGatewayClient implements PaymentGatewayClient {

    private final PaymentGatewayClient delegate;
    private final ExecutorService executor;
    private final long timeoutMillis;

    public TimeoutPaymentGatewayClient(PaymentGatewayClient delegate, ExecutorService executor, long timeoutMillis) {
        this.delegate = delegate;
        this.executor = executor;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public ApprovalResult approve(String customerId, Long amount) {
        Callable<ApprovalResult> task = () -> delegate.approve(customerId, amount);
        Future<ApprovalResult> future = executor.submit(task);
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new PaymentGatewayException("PG 응답 시간이 초과되었습니다.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PaymentGatewayException gatewayException) {
                throw gatewayException;
            }
            throw new PaymentGatewayException("PG 승인 처리 중 오류가 발생했습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("PG 승인 처리가 중단되었습니다.");
        }
    }
}