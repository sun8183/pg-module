package com.switchwon.payment.config;

import com.switchwon.payment.wallet.domain.CustomerWallet;
import com.switchwon.payment.wallet.repository.CustomerWalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String TEST_CUSTOMER_ID = "test-customer";
    private static final long TEST_INITIAL_BALANCE = 100_000L;

    private final CustomerWalletRepository walletRepository;

    public DataInitializer(CustomerWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        walletRepository.findByCustomerId(TEST_CUSTOMER_ID)
                .orElseGet(() -> walletRepository.save(new CustomerWallet(TEST_CUSTOMER_ID, TEST_INITIAL_BALANCE)));
        log.info("테스트 계정 준비 완료: customerId={}, balance={}", TEST_CUSTOMER_ID, TEST_INITIAL_BALANCE);
    }
}