package com.switchwon.payment.common.logging;

import com.switchwon.payment.wallet.domain.CustomerWallet;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// 트랜잭션 어드바이저(LOWEST_PRECEDENCE)보다 바깥에 둬서 커밋/롤백 이후에 로깅되도록 함
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Aspect
@Component
public class PaymentAuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("PAYMENT_AUDIT");

    @Pointcut("execution(* com.switchwon.payment.wallet.service.WalletService.charge(..)) || "
            + "execution(* com.switchwon.payment.wallet.service.WalletService.deductIfSufficient(..))")
    public void walletBalanceFlow() {
    }

    @Pointcut("execution(* com.switchwon.payment.payment.gateway.PaymentGatewayClient.approve(..))")
    public void pgApproval() {
    }

    @Around("walletBalanceFlow() || pgApproval()")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        String args = Arrays.toString(joinPoint.getArgs());
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = System.currentTimeMillis() - start;
            log.info("[{}] args={} result={} elapsedMs={}", method, args, describe(result), elapsedMs);
            return result;
        } catch (Throwable e) {
            long elapsedMs = System.currentTimeMillis() - start;
            log.warn("[{}] args={} elapsedMs={} failed={}", method, args, elapsedMs, e.toString());
            throw e;
        }
    }

    private Object describe(Object result) {
        if (result instanceof CustomerWallet wallet) {
            return "balance=" + wallet.getBalance();
        }
        return result;
    }
}
