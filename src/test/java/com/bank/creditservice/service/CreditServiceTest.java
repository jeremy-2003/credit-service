package com.bank.creditservice.service;

import com.bank.creditservice.event.CreditEventProducer;
import com.bank.creditservice.model.credit.Credit;
import com.bank.creditservice.model.credit.CreditType;
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
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CreditServiceTest {
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private CustomerClientService customerClientService;
    @Mock
    private CustomerCacheService customerCacheService;
    @Mock
    private CreditEventProducer creditEventProducer;
    @InjectMocks
    private CreditService creditService;
    private Credit testCredit;
    private Customer testCustomer;
    @BeforeEach
    void setUp() {
        testCredit = new Credit();
        testCredit.setId("1");
        testCredit.setCustomerId("customer1");
        testCredit.setCreditType(CreditType.PERSONAL);
        testCredit.setAmount(new BigDecimal("1000"));
        testCredit.setRemainingBalance(new BigDecimal("1000"));
        testCredit.setInterestRate(new BigDecimal("0.1"));
        testCredit.setCreatedAt(LocalDateTime.now());
        testCustomer = new Customer();
        testCustomer.setId("customer1");
        testCustomer.setCustomerType(CustomerType.PERSONAL);
    }
    @Test
    void createCredit_ValidPersonalCustomer_Success() {
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById(anyString())).thenReturn(Mono.just(testCustomer));
        when(customerCacheService.saveCustomer(anyString(), any())).thenReturn(Mono.empty());
        when(creditRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        when(creditRepository.save(any(Credit.class))).thenReturn(Mono.just(testCredit));
        doNothing().when(creditEventProducer).publishCreditCreated(any(Credit.class));
        StepVerifier.create(creditService.createCredit(testCredit))
                .expectNext(testCredit)
                .verifyComplete();
        verify(creditRepository).findByCustomerId(anyString());
        verify(creditRepository).save(any(Credit.class));
        verify(creditEventProducer).publishCreditCreated(any(Credit.class));
    }
    @Test
    void createCredit_PersonalCustomerWithExistingCredit_Error() {
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById(anyString())).thenReturn(Mono.just(testCustomer));
        when(customerCacheService.saveCustomer(anyString(), any())).thenReturn(Mono.empty());
        when(creditRepository.findByCustomerId(anyString())).thenReturn(Flux.just(testCredit));
        StepVerifier.create(creditService.createCredit(testCredit))
                .expectErrorMessage("Personal customer can only have one active credit")
                .verify();
    }
    @Test
    void createCredit_CustomerTypeMismatch_Error() {
        testCustomer.setCustomerType(CustomerType.BUSINESS);
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById(anyString())).thenReturn(Mono.just(testCustomer));
        when(customerCacheService.saveCustomer(anyString(), any())).thenReturn(Mono.empty());
        StepVerifier.create(creditService.createCredit(testCredit))
                .expectErrorMessage("Customer type does not match credit type")
                .verify();
    }
    @Test
    void getAllCredits_Success() {
        when(creditRepository.findAll()).thenReturn(Flux.just(testCredit));
        StepVerifier.create(creditService.getAllCredits())
                .expectNext(testCredit)
                .verifyComplete();
        verify(creditRepository).findAll();
    }
    @Test
    void getCreditsByCustomerId_Success() {
        when(creditRepository.findByCustomerId(anyString())).thenReturn(Flux.just(testCredit));
        StepVerifier.create(creditService.getCreditsByCustomerId("customer1"))
                .expectNext(testCredit)
                .verifyComplete();
        verify(creditRepository).findByCustomerId("customer1");
    }
    @Test
    void getCreditsByCustomerId_NoCreditsFound_Error() {
        when(creditRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(creditService.getCreditsByCustomerId("customer1"))
                .expectErrorMessage("This customer doesnt have credits")
                .verify();
        verify(creditRepository).findByCustomerId("customer1");
    }
    @Test
    void getCreditById_Success() {
        when(creditRepository.findById(anyString())).thenReturn(Mono.just(testCredit));
        StepVerifier.create(creditService.getCreditById("1"))
                .expectNext(testCredit)
                .verifyComplete();
        verify(creditRepository).findById("1");
    }
    @Test
    void getCreditById_NotFound_Error() {
        when(creditRepository.findById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(creditService.getCreditById("1"))
                .expectErrorMessage("Credit not found")
                .verify();
        verify(creditRepository).findById("1");
    }
    @Test
    void updateCredit_Success() {
        Credit updatedCredit = new Credit();
        updatedCredit.setAmount(new BigDecimal("2000"));
        updatedCredit.setInterestRate(new BigDecimal("0.15"));
        updatedCredit.setRemainingBalance(new BigDecimal("1800"));
        when(creditRepository.findById(anyString())).thenReturn(Mono.just(testCredit));
        when(creditRepository.save(any(Credit.class))).thenReturn(Mono.just(testCredit));
        doNothing().when(creditEventProducer).publishCreditUpdated(any(Credit.class));
        StepVerifier.create(creditService.updateCredit("1", updatedCredit))
                .expectNext(testCredit)
                .verifyComplete();
        verify(creditRepository).findById("1");
        verify(creditRepository).save(any(Credit.class));
        verify(creditEventProducer).publishCreditUpdated(any(Credit.class));
    }
    @Test
    void updateCredit_NotFound_Error() {
        when(creditRepository.findById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(creditService.updateCredit("1", testCredit))
                .expectErrorMessage("Credit not found")
                .verify();
        verify(creditRepository).findById("1");
        verify(creditRepository, never()).save(any(Credit.class));
    }
    @Test
    void deleteCredit_Success() {
        when(creditRepository.findById(anyString())).thenReturn(Mono.just(testCredit));
        when(creditRepository.deleteById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(creditService.deleteCredit("1"))
                .verifyComplete();
        verify(creditRepository).findById("1");
        verify(creditRepository).deleteById("1");
    }
    @Test
    void deleteCredit_NotFound_Error() {
        when(creditRepository.findById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(creditService.deleteCredit("1"))
                .expectErrorMessage("Credit not found")
                .verify();
        verify(creditRepository).findById("1");
        verify(creditRepository, never()).deleteById(anyString());
    }
    @Test
    void validateCustomer_CacheHit_Success() {
        // Configurar el mock del customer cache para devolver un cliente
        when(customerCacheService.getCustomer(anyString()))
                .thenReturn(Mono.just(testCustomer));
        // Configurar el mock del repositorio para simular que no hay créditos existentes
        when(creditRepository.findByCustomerId(anyString()))
                .thenReturn(Flux.empty());
        // Configurar el mock del repositorio para el save
        when(creditRepository.save(any(Credit.class)))
                .thenReturn(Mono.just(testCredit));
        // Configurar el mock del productor de eventos
        doNothing().when(creditEventProducer).publishCreditCreated(any(Credit.class));
        StepVerifier.create(creditService.createCredit(testCredit))
                .expectNext(testCredit)
                .verifyComplete();
        // Verificar que no se llamó al servicio de cliente
        verify(customerClientService, never()).getCustomerById(anyString());
        verify(creditRepository).findByCustomerId(anyString());
        verify(creditRepository).save(any(Credit.class));
    }
    @Test
    void validateCustomer_CacheMissServiceSuccess_Success() {
        // Configurar el mock del cache para simular cache miss
        when(customerCacheService.getCustomer(anyString()))
                .thenReturn(Mono.empty());
        // Configurar el mock del servicio de cliente
        when(customerClientService.getCustomerById(anyString()))
                .thenReturn(Mono.just(testCustomer));
        // Configurar el mock del cache para guardar el cliente
        when(customerCacheService.saveCustomer(anyString(), any()))
                .thenReturn(Mono.empty());
        // Configurar el mock del repositorio para simular que no hay créditos existentes
        when(creditRepository.findByCustomerId(anyString()))
                .thenReturn(Flux.empty());
        // Configurar el mock del repositorio para el save
        when(creditRepository.save(any(Credit.class)))
                .thenReturn(Mono.just(testCredit));
        // Configurar el mock del productor de eventos
        doNothing().when(creditEventProducer).publishCreditCreated(any(Credit.class));
        StepVerifier.create(creditService.createCredit(testCredit))
                .expectNext(testCredit)
                .verifyComplete();
        verify(customerClientService).getCustomerById(anyString());
        verify(customerCacheService).saveCustomer(anyString(), any());
        verify(creditRepository).findByCustomerId(anyString());
        verify(creditRepository).save(any(Credit.class));
    }
    @Test
    void validateCustomer_CacheAndServiceError_Error() {
        when(customerCacheService.getCustomer(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Cache error")));

        when(customerClientService.getCustomerById(anyString()))
                .thenReturn(Mono.empty());
        StepVerifier.create(creditService.createCredit(testCredit))
                .verifyComplete();
        verify(customerCacheService).getCustomer(anyString());
        verify(customerClientService).getCustomerById(anyString());
        verify(creditRepository, never()).save(any(Credit.class));
    }
}