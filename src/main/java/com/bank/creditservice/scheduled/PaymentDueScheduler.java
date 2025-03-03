package com.bank.creditservice.scheduled;

import com.bank.creditservice.model.credit.CreditStatus;
import com.bank.creditservice.model.creditcard.PaymentStatus;
import com.bank.creditservice.repository.CreditCardRepository;
import com.bank.creditservice.repository.CreditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
@Slf4j
@Component
@EnableScheduling
public class PaymentDueScheduler {
    @Autowired
    private CreditRepository creditRepository;
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Scheduled(cron = "10 14 01 * * ?")
    public void checkOverduePayments() {
        log.info("Starting daily check for overdue payments");
        LocalDateTime now = LocalDateTime.now();
        updateOverdueCredits(now);
        updateOverdueCreditCards(now);
        log.info("Completed daily check for overdue payments");
    }
    private void updateOverdueCredits(LocalDateTime now) {
        log.info("Checking for overdue credits...");
        creditRepository.findAll()
                .filter(credit ->
                        credit.getCreditStatus() == CreditStatus.ACTIVE &&
                                credit.getPaymentStatus() == PaymentStatus.PENDING &&
                                credit.getNextPaymentDate() != null)

                .filter(credit -> credit.getNextPaymentDate().isBefore(now))
                .flatMap(credit -> {
                    log.info("Credit {} is overdue. Payment date was: {}",
                            credit.getId(), credit.getNextPaymentDate());
                    credit.setPaymentStatus(PaymentStatus.OVERDUE);
                    credit.setModifiedAt(now);

                    long daysLate = java.time.Duration.between(
                            credit.getNextPaymentDate(), now).toDays();
                    if (daysLate > 90) {
                        log.warn("Credit {} is severely overdue ({} days). Marking as DEFAULTED.",
                                credit.getId(), daysLate);
                        credit.setCreditStatus(CreditStatus.DEFAULTED);
                    }
                    return creditRepository.save(credit);
                })
                .doOnNext(updatedCredit ->
                        log.info("Updated credit {} status to OVERDUE", updatedCredit.getId()))
                .doOnError(error ->
                        log.error("Error updating overdue credits: {}", error.getMessage()))
                .subscribe();
    }
    private void updateOverdueCreditCards(LocalDateTime now) {
        log.info("Checking for overdue credit cards...");
        creditCardRepository.findAll()
                .filter(card ->
                        "ACTIVE".equals(card.getStatus()) &&
                                card.getPaymentStatus() == PaymentStatus.PENDING &&
                                card.getPaymentDueDate() != null)
                .filter(card -> card.getPaymentDueDate().isBefore(now))
                .flatMap(card -> {
                    log.info("Credit card {} is overdue. Payment due date was: {}",
                            card.getId(), card.getPaymentDueDate());
                    card.setPaymentStatus(PaymentStatus.OVERDUE);
                    card.setModifiedAt(now);
                    long daysLate = java.time.Duration.between(
                            card.getPaymentDueDate(), now).toDays();
                    if (daysLate > 60) {
                        log.warn("Credit card {} is severely overdue ({} days). Blocking card.",
                                card.getId(), daysLate);
                        card.setStatus("BLOCKED");
                    }
                    return creditCardRepository.save(card);
                })
                .doOnNext(updatedCard ->
                        log.info("Updated credit card {} status to OVERDUE", updatedCard.getId()))
                .doOnError(error ->
                        log.error("Error updating overdue credit cards: {}", error.getMessage()))
                .subscribe();
    }
}
