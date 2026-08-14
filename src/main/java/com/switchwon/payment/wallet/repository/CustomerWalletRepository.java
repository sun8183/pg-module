package com.switchwon.payment.wallet.repository;

import com.switchwon.payment.wallet.domain.CustomerWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerWalletRepository extends JpaRepository<CustomerWallet, Long> {

    Optional<CustomerWallet> findByCustomerId(String customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerWallet w
            set w.balance = w.balance - :amount, w.updatedAt = CURRENT_TIMESTAMP
            where w.customerId = :customerId and w.balance >= :amount
            """)
    int deductIfSufficient(@Param("customerId") String customerId, @Param("amount") Long amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerWallet w
            set w.balance = w.balance + :amount, w.updatedAt = CURRENT_TIMESTAMP
            where w.customerId = :customerId
            """)
    int charge(@Param("customerId") String customerId, @Param("amount") Long amount);
}