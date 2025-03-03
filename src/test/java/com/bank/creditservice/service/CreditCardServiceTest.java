package com.bank.creditservice.service;
import com.bank.creditservice.client.AccountClientService;
import com.bank.creditservice.client.CustomerClientService;
import com.bank.creditservice.event.CreditCardEventProducer;
import com.bank.creditservice.model.account.Account;
import com.bank.creditservice.model.account.AccountType;
import com.bank.creditservice.model.creditcard.CreditCard;
import com.bank.creditservice.model.creditcard.CreditCardType;
import com.bank.creditservice.model.creditcard.PaymentStatus;
import com.bank.creditservice.model.customer.Customer;
import com.bank.creditservice.model.customer.CustomerType;
import com.bank.creditservice.repository.CreditCardRepository;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CustomerCacheService customerCacheService;
    @Mock
    private CustomerClientService customerClientService;
    @Mock
    private CreditCardEventProducer creditCardEventProducer;
    @Mock
    private AccountClientService accountClientService;
    @Mock
    private CustomerEligibilityService customerEligibilityService;
    @InjectMocks
    private CreditCardService creditCardService;
    private CreditCard testCreditCard;
    private Customer testCustomer;
    private Account testAccount;
    @BeforeEach
    void setUp() {
        testCreditCard = createTestCreditCard();
        testCustomer = createTestCustomer();
        testAccount = createTestAccount();
    }
    private CreditCard createTestCreditCard() {
        CreditCard creditCard = new CreditCard();
        creditCard.setId("1");
        creditCard.setCustomerId("customer1");
        creditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        creditCard.setCreditLimit(new BigDecimal("5000"));
        creditCard.setAvailableBalance(new BigDecimal("5000"));
        creditCard.setStatus("ACTIVE");
        return creditCard;
    }
    private Customer createTestCustomer() {
        Customer customer = new Customer();
        customer.setId("customer1");
        customer.setFullName("Test Customer");
        customer.setCustomerType(CustomerType.PERSONAL);
        return customer;
    }
    private Account createTestAccount() {
        Account account = new Account();
        account.setId("account1");
        account.setCustomerId("customer1");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(1000);
        return account;
    }
    @Test
    void createCreditCard_PersonalCustomerWithSavingsAccount_Success() {
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById(anyString())).thenReturn(Mono.just(testCustomer));
        when(customerCacheService.saveCustomer(anyString(), any())).thenReturn(Mono.empty());
        when(accountClientService.getAccountsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.singletonList(testAccount)));
        when(accountClientService.updateVipPymStatus(anyString(), anyBoolean(), anyString()))
                .thenReturn(Mono.just(testAccount));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean()))
                .thenReturn(Mono.just(testCustomer));
        when(creditCardRepository.save(any(CreditCard.class)))
                .thenAnswer(invocation -> {
                    CreditCard card = invocation.getArgument(0);
                    return Mono.just(card);
                });
        StepVerifier.create(creditCardService.createCreditCard(testCreditCard))
                .assertNext(card -> {
                    assertEquals("ACTIVE", card.getStatus());
                    assertEquals(PaymentStatus.PAID, card.getPaymentStatus());
                    assertNotNull(card.getCreatedAt());
                    assertNotNull(card.getCutoffDate());
                    assertNotNull(card.getPaymentDueDate());
                })
                .verifyComplete();
        verify(creditCardEventProducer).publishCreditCardCreated(any());
        verify(accountClientService).updateVipPymStatus(eq("account1"), eq(true), eq("VIP"));
        verify(customerClientService).updateVipPymStatus(eq("customer1"), eq(true));
    }
    @Test
    void createCreditCard_CustomerTypeMismatch_Error() {
        testCustomer.setCustomerType(CustomerType.BUSINESS);
        testCreditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById(anyString())).thenReturn(Mono.just(testCustomer));
        when(customerCacheService.saveCustomer(anyString(), any())).thenReturn(Mono.empty());
        StepVerifier.create(creditCardService.createCreditCard(testCreditCard))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Customer type does not match credit card type"))
                .verify();
        verify(creditCardRepository, never()).save(any());
        verify(creditCardEventProducer, never()).publishCreditCardCreated(any());
    }
    @Test
    void createCreditCard_BusinessCustomerWithCheckingAccount_Success() {
        testCustomer.setCustomerType(CustomerType.BUSINESS);
        testCreditCard.setCardType(CreditCardType.BUSINESS_CREDIT_CARD);
        testAccount.setAccountType(AccountType.CHECKING);
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById(anyString())).thenReturn(Mono.just(testCustomer));
        when(customerCacheService.saveCustomer(anyString(), any())).thenReturn(Mono.empty());
        when(accountClientService.getAccountsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.singletonList(testAccount)));
        when(accountClientService.updateVipPymStatus(anyString(), anyBoolean(), anyString()))
                .thenReturn(Mono.just(testAccount));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean()))
                .thenReturn(Mono.just(testCustomer));
        when(creditCardRepository.save(any(CreditCard.class)))
                .thenAnswer(invocation -> {
                    CreditCard card = invocation.getArgument(0);
                    return Mono.just(card);
                });
        StepVerifier.create(creditCardService.createCreditCard(testCreditCard))
                .assertNext(card -> {
                    assertEquals("ACTIVE", card.getStatus());
                    assertEquals(PaymentStatus.PAID, card.getPaymentStatus());
                    assertNotNull(card.getCreatedAt());
                    assertNotNull(card.getCutoffDate());
                    assertNotNull(card.getPaymentDueDate());
                })
                .verifyComplete();
        verify(creditCardEventProducer).publishCreditCardCreated(any());
        verify(accountClientService).updateVipPymStatus(eq("account1"), eq(true), eq("PYM"));
        verify(customerClientService).updateVipPymStatus(eq("customer1"), eq(true));
    }
    @Test
    void createCreditCard_CustomerHasOverdueDebt_Error() {
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(true));
        StepVerifier.create(creditCardService.createCreditCard(testCreditCard))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Customer has overdue debt"))
                .verify();
        verify(creditCardRepository, never()).save(any());
        verify(creditCardEventProducer, never()).publishCreditCardCreated(any());
    }
    @Test
    void createCreditCard_PersonalCustomerWithoutAccounts_Success() {
        when(customerEligibilityService.hasOverdueDebt(anyString())).thenReturn(Mono.just(false));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(testCustomer));
        when(accountClientService.getAccountsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.emptyList()));
        when(creditCardRepository.save(any(CreditCard.class)))
                .thenAnswer(invocation -> {
                    CreditCard card = invocation.getArgument(0);
                    return Mono.just(card);
                });
        StepVerifier.create(creditCardService.createCreditCard(testCreditCard))
                .assertNext(card -> {
                    assertEquals("ACTIVE", card.getStatus());
                    assertEquals(PaymentStatus.PAID, card.getPaymentStatus());
                    assertNotNull(card.getCreatedAt());
                    assertNotNull(card.getCutoffDate());
                    assertNotNull(card.getPaymentDueDate());
                })
                .verifyComplete();
        verify(creditCardEventProducer).publishCreditCardCreated(any());
        verify(accountClientService, never()).updateVipPymStatus(anyString(), anyBoolean(), anyString());
        verify(customerClientService, never()).updateVipPymStatus(anyString(), anyBoolean());
    }
    @Test
    void getCreditCardById_Success() {
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.just(testCreditCard));
        StepVerifier.create(creditCardService.getCreditCardById("1"))
                .expectNext(testCreditCard)
                .verifyComplete();
    }
    @Test
    void getCreditCardById_NotFound_Error() {
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(creditCardService.getCreditCardById("1"))
                .expectErrorMessage("This credit card doesn exist")
                .verify();
    }
    @Test
    void getCreditCardsByCustomerId_Success() {
        when(creditCardRepository.findByCustomerId(anyString()))
                .thenReturn(Flux.just(testCreditCard));
        StepVerifier.create(creditCardService.getCreditCardsByCustomerId("customer1"))
                .expectNext(testCreditCard)
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomerId_NotFound_Error() {
        when(creditCardRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(creditCardService.getCreditCardsByCustomerId("customer1"))
                .expectErrorMessage("This customer doesnt have credit cards")
                .verify();
    }
    @Test
    void updateCreditCard_Success() {
        CreditCard updatedCard = createTestCreditCard();
        updatedCard.setCreditLimit(new BigDecimal("10000"));
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.just(testCreditCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(Mono.just(updatedCard));
        StepVerifier.create(creditCardService.updateCreditCard("1", updatedCard))
                .expectNext(updatedCard)
                .verifyComplete();
        verify(creditCardEventProducer).publishCreditCardUpdated(any());
    }
    @Test
    void deleteCreditCard_LastCardPersonalCustomer_Success() {
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.just(testCreditCard));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(testCustomer));
        when(creditCardRepository.deleteById(anyString())).thenReturn(Mono.empty());
        when(creditCardRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        when(accountClientService.getAccountsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.singletonList(testAccount)));
        when(accountClientService.updateVipPymStatus(anyString(), anyBoolean(), anyString()))
                .thenReturn(Mono.just(testAccount));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean()))
                .thenReturn(Mono.just(testCustomer));
        StepVerifier.create(creditCardService.deleteCreditCard("1"))
                .verifyComplete();
        verify(creditCardRepository).deleteById("1");
        verify(customerClientService).updateVipPymStatus(anyString(), eq(false));
        verify(accountClientService).updateVipPymStatus(anyString(), eq(false), eq("VIP"));
    }
    @Test
    void deleteCreditCard_LastCardBusinessCustomer_Success() {
        testCustomer.setCustomerType(CustomerType.BUSINESS);
        testAccount.setAccountType(AccountType.CHECKING);
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.just(testCreditCard));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(testCustomer));
        when(creditCardRepository.deleteById(anyString())).thenReturn(Mono.empty());
        when(creditCardRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        when(accountClientService.getAccountsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.singletonList(testAccount)));
        when(accountClientService.updateVipPymStatus(anyString(), anyBoolean(), anyString()))
                .thenReturn(Mono.just(testAccount));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean()))
                .thenReturn(Mono.just(testCustomer));
        StepVerifier.create(creditCardService.deleteCreditCard("1"))
                .verifyComplete();
        verify(creditCardRepository).deleteById("1");
        verify(customerClientService).updateVipPymStatus(anyString(), eq(false));
        verify(accountClientService).updateVipPymStatus(anyString(), eq(false), eq("PYM"));
    }
    @Test
    void deleteCreditCard_NotLastCard_Success() {
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.just(testCreditCard));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(testCustomer));
        when(creditCardRepository.deleteById(anyString())).thenReturn(Mono.empty());
        when(creditCardRepository.findByCustomerId(anyString()))
                .thenReturn(Flux.just(createTestCreditCard()));
        StepVerifier.create(creditCardService.deleteCreditCard("1"))
                .verifyComplete();
        verify(creditCardRepository).deleteById("1");
        verify(customerClientService, never()).updateVipPymStatus(anyString(), anyBoolean());
        verify(accountClientService, never()).updateVipPymStatus(anyString(), anyBoolean(), anyString());
    }
    @Test
    void deleteCreditCard_NotFound_Error() {
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(creditCardService.deleteCreditCard("1"))
                .expectErrorMessage("Credit card not found")
                .verify();
        verify(creditCardRepository, never()).deleteById(anyString());
    }
    @Test
    void deleteCreditCard_WithoutAccounts_Success() {
        when(creditCardRepository.findById(anyString())).thenReturn(Mono.just(testCreditCard));
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(testCustomer));
        when(creditCardRepository.deleteById(anyString())).thenReturn(Mono.empty());
        when(creditCardRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        when(accountClientService.getAccountsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.emptyList()));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean()))
                .thenReturn(Mono.just(testCustomer));
        StepVerifier.create(creditCardService.deleteCreditCard("1"))
                .verifyComplete();
        verify(creditCardRepository).deleteById("1");
        verify(customerClientService).updateVipPymStatus(anyString(), eq(false));
        verify(accountClientService, never()).updateVipPymStatus(anyString(), anyBoolean(), anyString());
    }
    @Test
    void getAllCreditCards_Success() {
        when(creditCardRepository.findAll()).thenReturn(Flux.just(testCreditCard));
        StepVerifier.create(creditCardService.getAllCreditCards())
                .expectNext(testCreditCard)
                .verifyComplete();
    }
}