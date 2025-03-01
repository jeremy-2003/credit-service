package com.bank.creditservice.service;

import com.bank.creditservice.model.credit.CreditStatus;
import com.bank.creditservice.model.creditcard.PaymentStatus;
import com.bank.creditservice.repository.CreditCardRepository;
import com.bank.creditservice.repository.CreditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomerEligibilityService {
    @Autowired
    private CreditRepository creditRepository;
    @Autowired
    private CreditCardRepository creditCardRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerEligibilityService.class);

    public Mono<Boolean> hasOverdueDebt(String customerId) {
        log.info("Checking if customer {} has overdue debt", customerId);
        Mono<Boolean> hasOverdueCredit = creditRepository.findByCustomerId(customerId)
                .filter(credit ->
                        credit.getCreditStatus() == CreditStatus.ACTIVE &&
                                credit.getPaymentStatus() == PaymentStatus.OVERDUE)
                .hasElements()
                .doOnNext(hasOverdue -> {
                    if (hasOverdue) {
                        log.info("Customer {} has overdue credits", customerId);
                    }
                });

        Mono<Boolean> hasOverdueCreditCard = creditCardRepository.findByCustomerId(customerId)
                .filter(card ->
                        "ACTIVE".equals(card.getStatus()) &&
                                card.getPaymentStatus() == PaymentStatus.OVERDUE)
                .hasElements()
                .doOnNext(hasOverdue -> {
                    if (hasOverdue) {
                        log.info("Customer {} has overdue credit cards", customerId);
                    }
                });

        return Mono.zip(hasOverdueCredit, hasOverdueCreditCard)
                .map(tuple -> tuple.getT1() || tuple.getT2())
                .doOnNext(result -> log.info("Customer {} has overdue debt: {}", customerId, result));
    }

    public Mono<Boolean> isCustomerEligibleForNewProduct(String customerId) {
        return hasOverdueDebt(customerId).map(hasDebt -> !hasDebt);
    }
}
