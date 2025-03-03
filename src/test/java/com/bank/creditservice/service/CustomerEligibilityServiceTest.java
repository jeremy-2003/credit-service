package com.bank.creditservice.service;

import com.bank.creditservice.model.credit.Credit;
import com.bank.creditservice.model.credit.CreditStatus;
import com.bank.creditservice.model.creditcard.CreditCard;
import com.bank.creditservice.model.creditcard.PaymentStatus;
import com.bank.creditservice.repository.CreditCardRepository;
import com.bank.creditservice.repository.CreditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class CustomerEligibilityServiceTest {
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @InjectMocks
    private CustomerEligibilityService customerEligibilityService;
    private Credit activeOverdueCredit;
    private Credit activePendingCredit;
    private Credit finishedCredit;
    private CreditCard activeOverdueCard;
    private CreditCard activePendingCard;
    private CreditCard inactiveCard;
    @BeforeEach
    void setUp() {
        activeOverdueCredit = new Credit();
        activeOverdueCredit.setId("credit1");
        activeOverdueCredit.setCustomerId("customer1");
        activeOverdueCredit.setCreditStatus(CreditStatus.ACTIVE);
        activeOverdueCredit.setPaymentStatus(PaymentStatus.OVERDUE);
        activePendingCredit = new Credit();
        activePendingCredit.setId("credit2");
        activePendingCredit.setCustomerId("customer1");
        activePendingCredit.setCreditStatus(CreditStatus.ACTIVE);
        activePendingCredit.setPaymentStatus(PaymentStatus.PENDING);
        finishedCredit = new Credit();
        finishedCredit.setId("credit3");
        finishedCredit.setCustomerId("customer1");
        finishedCredit.setCreditStatus(CreditStatus.FINISHED);
        finishedCredit.setPaymentStatus(PaymentStatus.PAID);
        activeOverdueCard = new CreditCard();
        activeOverdueCard.setId("card1");
        activeOverdueCard.setCustomerId("customer1");
        activeOverdueCard.setStatus("ACTIVE");
        activeOverdueCard.setPaymentStatus(PaymentStatus.OVERDUE);
        activePendingCard = new CreditCard();
        activePendingCard.setId("card2");
        activePendingCard.setCustomerId("customer1");
        activePendingCard.setStatus("ACTIVE");
        activePendingCard.setPaymentStatus(PaymentStatus.PENDING);
        inactiveCard = new CreditCard();
        inactiveCard.setId("card3");
        inactiveCard.setCustomerId("customer1");
        inactiveCard.setStatus("INACTIVE");
        inactiveCard.setPaymentStatus(PaymentStatus.OVERDUE);
    }
    @Test
    void hasOverdueDebt_CustomerWithOverdueCredit_ReturnsTrue() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activeOverdueCredit));
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activePendingCard));
        // Act & Assert
        StepVerifier.create(customerEligibilityService.hasOverdueDebt("customer1"))
                .expectNext(true)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void hasOverdueDebt_CustomerWithOverdueCreditCard_ReturnsTrue() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activePendingCredit));
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activeOverdueCard));
        // Act & Assert
        StepVerifier.create(customerEligibilityService.hasOverdueDebt("customer1"))
                .expectNext(true)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void hasOverdueDebt_CustomerWithBothOverdue_ReturnsTrue() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activeOverdueCredit));
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activeOverdueCard));
        // Act & Assert
        StepVerifier.create(customerEligibilityService.hasOverdueDebt("customer1"))
                .expectNext(true)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void hasOverdueDebt_CustomerWithNoOverdueDebt_ReturnsFalse() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activePendingCredit, finishedCredit));
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activePendingCard, inactiveCard));
        // Act & Assert
        StepVerifier.create(customerEligibilityService.hasOverdueDebt("customer1"))
                .expectNext(false)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void hasOverdueDebt_CustomerWithInactiveOverdueProducts_ReturnsFalse() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(finishedCredit));
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(inactiveCard));
        // Act & Assert
        StepVerifier.create(customerEligibilityService.hasOverdueDebt("customer1"))
                .expectNext(false)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void hasOverdueDebt_CustomerWithNoProducts_ReturnsFalse() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.empty());
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.empty());
        // Act & Assert
        StepVerifier.create(customerEligibilityService.hasOverdueDebt("customer1"))
                .expectNext(false)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void isCustomerEligibleForNewProduct_CustomerWithOverdueDebt_ReturnsFalse() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activeOverdueCredit));
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activePendingCard));
        // Act & Assert
        StepVerifier.create(customerEligibilityService.isCustomerEligibleForNewProduct("customer1"))
                .expectNext(false)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void isCustomerEligibleForNewProduct_CustomerWithNoOverdueDebt_ReturnsTrue() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activePendingCredit));
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(activePendingCard));
        // Act & Assert
        StepVerifier.create(customerEligibilityService.isCustomerEligibleForNewProduct("customer1"))
                .expectNext(true)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
    @Test
    void isCustomerEligibleForNewProduct_CustomerWithNoProducts_ReturnsTrue() {
        // Arrange
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.empty());
        when(creditCardRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.empty());
        // Act & Assert
        StepVerifier.create(customerEligibilityService.isCustomerEligibleForNewProduct("customer1"))
                .expectNext(true)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
        verify(creditCardRepository).findByCustomerId("customer1");
    }
}
