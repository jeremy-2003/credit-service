package com.bank.creditservice.service;

import com.bank.creditservice.client.CustomerClientService;
import com.bank.creditservice.event.CreditEventProducer;
import com.bank.creditservice.model.credit.Credit;
import com.bank.creditservice.model.credit.CreditStatus;
import com.bank.creditservice.model.credit.CreditType;
import com.bank.creditservice.model.creditcard.PaymentStatus;
import com.bank.creditservice.model.customer.Customer;
import com.bank.creditservice.model.customer.CustomerType;
import com.bank.creditservice.repository.CreditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditServiceTest {
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private CustomerCacheService customerCacheService;
    @Mock
    private CustomerClientService customerClientService;
    @Mock
    private CreditEventProducer creditEventProducer;
    @Mock
    private CustomerEligibilityService customerEligibilityService;
    @InjectMocks
    private CreditService creditService;
    private Customer personalCustomer;
    private Customer businessCustomer;
    private Credit personalCredit;
    private Credit businessCredit;
    @BeforeEach
    void setUp() {
        personalCustomer = new Customer();
        personalCustomer.setId("personal123");
        personalCustomer.setFullName("John Doe");
        personalCustomer.setCustomerType(CustomerType.PERSONAL);
        personalCustomer.setEmail("john@example.com");
        businessCustomer = new Customer();
        businessCustomer.setId("business456");
        businessCustomer.setFullName("Acme Inc");
        businessCustomer.setCustomerType(CustomerType.BUSINESS);
        businessCustomer.setEmail("contact@acme.com");
        personalCredit = new Credit();
        personalCredit.setId("credit123");
        personalCredit.setCustomerId("personal123");
        personalCredit.setCreditType(CreditType.PERSONAL);
        personalCredit.setAmount(new BigDecimal("1000.00"));
        personalCredit.setInterestRate(new BigDecimal("0.05"));
        businessCredit = new Credit();
        businessCredit.setId("credit456");
        businessCredit.setCustomerId("business456");
        businessCredit.setCreditType(CreditType.BUSINESS);
        businessCredit.setAmount(new BigDecimal("5000.00"));
        businessCredit.setInterestRate(new BigDecimal("0.08"));
    }
    @Test
    void createCredit_ValidPersonalCreditForPersonalCustomer_ReturnsCredit() {
        // Arrange
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(personalCustomer));
        when(creditRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> {
            Credit savedCredit = invocation.getArgument(0);
            savedCredit.setId("generated-id");
            return Mono.just(savedCredit);
        });
        doNothing().when(creditEventProducer).publishCreditCreated(any(Credit.class));
        // Act & Assert
        StepVerifier.create(creditService.createCredit(personalCredit))
                .assertNext(credit -> {
                    assert credit.getId().equals("generated-id");
                    assert credit.getCreditStatus() == CreditStatus.ACTIVE;
                    assert credit.getPaymentStatus() == PaymentStatus.PENDING;
                    assert credit.getRemainingBalance().equals(personalCredit.getAmount());
                    assert credit.getMinimumPayment()
                        .equals(personalCredit.getAmount().multiply(new BigDecimal("0.10")));
                    assert credit.getNextPaymentDate() != null;
                })
                .verifyComplete();
        verify(creditEventProducer, times(1)).publishCreditCreated(any(Credit.class));
    }
    @Test
    void createCredit_ValidBusinessCreditForBusinessCustomer_ReturnsCredit() {
        // Arrange
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(businessCustomer));
        when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> {
            Credit savedCredit = invocation.getArgument(0);
            savedCredit.setId("generated-id");
            return Mono.just(savedCredit);
        });
        doNothing().when(creditEventProducer).publishCreditCreated(any(Credit.class));
        // Act & Assert
        StepVerifier.create(creditService.createCredit(businessCredit))
                .assertNext(credit -> {
                    assert credit.getId().equals("generated-id");
                    assert credit.getCreditStatus() == CreditStatus.ACTIVE;
                    assert credit.getPaymentStatus() == PaymentStatus.PENDING;
                    assert credit.getRemainingBalance().equals(businessCredit.getAmount());
                    assert credit.getMinimumPayment()
                        .equals(businessCredit.getAmount().multiply(new BigDecimal("0.10")));
                    assert credit.getNextPaymentDate() != null;
                })
                .verifyComplete();
        verify(creditEventProducer, times(1)).publishCreditCreated(any(Credit.class));
    }
    @Test
    void createCredit_CustomerHasOverdueDebt_ReturnsError() {
        // Arrange
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(true));
        // Act & Assert
        StepVerifier.create(creditService.createCredit(personalCredit))
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().contains("Customer has overdue debt"))
                .verify();
        verify(creditEventProducer, never()).publishCreditCreated(any(Credit.class));
    }
    @Test
    void createCredit_PersonalCustomerAlreadyHasCredit_ReturnsError() {
        // Arrange
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(personalCustomer));
        when(creditRepository.findByCustomerId(anyString())).thenReturn(Flux.just(personalCredit));
        // Act & Assert
        StepVerifier.create(creditService.createCredit(personalCredit))
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().contains("Personal customer can only have one active credit"))
                .verify();
        verify(creditEventProducer, never()).publishCreditCreated(any(Credit.class));
    }
    @Test
    void createCredit_MismatchedCustomerAndCreditType_ReturnsError() {
        // Arrange
        Credit mismatchedCredit = new Credit();
        mismatchedCredit.setCustomerId("personal123");
        mismatchedCredit.setCreditType(CreditType.BUSINESS);
        mismatchedCredit.setAmount(new BigDecimal("1000.00"));
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(personalCustomer));
        // Act & Assert
        StepVerifier.create(creditService.createCredit(mismatchedCredit))
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().contains("Customer type does not match credit type"))
                .verify();
        verify(creditEventProducer, never()).publishCreditCreated(any(Credit.class));
    }
    @Test
    void createCredit_CustomerNotInCacheButInService_ReturnsCredit() {
        // Arrange
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById(anyString())).thenReturn(Mono.just(personalCustomer));
        when(customerCacheService.saveCustomer(anyString(), any(Customer.class))).thenReturn(Mono.empty());
        when(creditRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> {
            Credit savedCredit = invocation.getArgument(0);
            savedCredit.setId("generated-id");
            return Mono.just(savedCredit);
        });
        doNothing().when(creditEventProducer).publishCreditCreated(any(Credit.class));
        // Act & Assert
        StepVerifier.create(creditService.createCredit(personalCredit))
                .assertNext(credit -> {
                    assert credit.getId().equals("generated-id");
                    assert credit.getCreditStatus() == CreditStatus.ACTIVE;
                })
                .verifyComplete();
    }
    @Test
    void getAllCredits_ReturnsAllCredits() {
        // Arrange
        when(creditRepository.findAll()).thenReturn(Flux.just(personalCredit, businessCredit));
        // Act & Assert
        StepVerifier.create(creditService.getAllCredits())
                .expectNext(personalCredit)
                .expectNext(businessCredit)
                .verifyComplete();
    }
    @Test
    void getCreditsByCustomerId_CustomerHasCredits_ReturnsCredits() {
        // Arrange
        when(creditRepository.findByCustomerId("personal123")).thenReturn(Flux.just(personalCredit));
        // Act & Assert
        StepVerifier.create(creditService.getCreditsByCustomerId("personal123"))
                .expectNext(personalCredit)
                .verifyComplete();
    }
    @Test
    void getCreditsByCustomerId_CustomerHasNoCredits_ReturnsError() {
        // Arrange
        when(creditRepository.findByCustomerId("nonexistent")).thenReturn(Flux.empty());
        // Act & Assert
        StepVerifier.create(creditService.getCreditsByCustomerId("nonexistent"))
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().contains("This customer doesnt have credits"))
                .verify();
    }
    @Test
    void getCreditById_CreditExists_ReturnsCredit() {
        // Arrange
        when(creditRepository.findById("credit123")).thenReturn(Mono.just(personalCredit));
        // Act & Assert
        StepVerifier.create(creditService.getCreditById("credit123"))
                .expectNext(personalCredit)
                .verifyComplete();
    }
    @Test
    void getCreditById_CreditDoesNotExist_ReturnsError() {
        // Arrange
        when(creditRepository.findById("nonexistent")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(creditService.getCreditById("nonexistent"))
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().contains("Credit not found"))
                .verify();
    }
    @Test
    void updateCredit_CreditExists_ReturnsUpdatedCredit() {
        // Arrange
        Credit existingCredit = new Credit();
        existingCredit.setId("credit123");
        existingCredit.setCustomerId("personal123");
        existingCredit.setCreditType(CreditType.PERSONAL);
        existingCredit.setAmount(new BigDecimal("1000.00"));
        existingCredit.setRemainingBalance(new BigDecimal("800.00"));
        existingCredit.setInterestRate(new BigDecimal("0.05"));
        existingCredit.setCreditStatus(CreditStatus.ACTIVE);
        existingCredit.setPaymentStatus(PaymentStatus.PENDING);
        Credit updatedCreditDetails = new Credit();
        updatedCreditDetails.setAmount(new BigDecimal("1200.00"));
        updatedCreditDetails.setRemainingBalance(new BigDecimal("1000.00"));
        updatedCreditDetails.setInterestRate(new BigDecimal("0.06"));
        updatedCreditDetails.setPaymentStatus(PaymentStatus.PAID);
        when(creditRepository.findById("credit123")).thenReturn(Mono.just(existingCredit));
        when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        doNothing().when(creditEventProducer).publishCreditUpdated(any(Credit.class));
        // Act & Assert
        StepVerifier.create(creditService.updateCredit("credit123", updatedCreditDetails))
                .assertNext(credit -> {
                    assert credit.getId().equals("credit123");
                    assert credit.getAmount().equals(new BigDecimal("1200.00"));
                    assert credit.getRemainingBalance().equals(new BigDecimal("1000.00"));
                    assert credit.getInterestRate().equals(new BigDecimal("0.06"));
                    assert credit.getPaymentStatus() == PaymentStatus.PAID;
                    assert credit.getCreditStatus() == CreditStatus.ACTIVE;
                    assert credit.getModifiedAt() != null;
                })
                .verifyComplete();
        verify(creditEventProducer, times(1)).publishCreditUpdated(any(Credit.class));
    }
    @Test
    void updateCredit_CreditDoesNotExist_ReturnsError() {
        // Arrange
        when(creditRepository.findById("nonexistent")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(creditService.updateCredit("nonexistent", personalCredit))
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().contains("Credit not found"))
                .verify();
        verify(creditEventProducer, never()).publishCreditUpdated(any(Credit.class));
    }
    @Test
    void deleteCredit_CreditExists_DeletesCredit() {
        // Arrange
        when(creditRepository.findById("credit123")).thenReturn(Mono.just(personalCredit));
        when(creditRepository.deleteById("credit123")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(creditService.deleteCredit("credit123"))
                .verifyComplete();
        verify(creditRepository, times(1)).deleteById("credit123");
    }
    @Test
    void deleteCredit_CreditDoesNotExist_ReturnsError() {
        // Arrange
        when(creditRepository.findById("nonexistent")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(creditService.deleteCredit("nonexistent"))
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().contains("Credit not found"))
                .verify();
        verify(creditRepository, never()).deleteById(anyString());
    }
}
