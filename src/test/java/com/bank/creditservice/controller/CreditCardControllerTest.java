package com.bank.creditservice.controller;

import static org.mockito.Mockito.*;

import com.bank.creditservice.model.creditcard.CreditCard;
import com.bank.creditservice.model.creditcard.CreditCardType;
import com.bank.creditservice.model.creditcard.PaymentStatus;
import com.bank.creditservice.service.CreditCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@ExtendWith(MockitoExtension.class)
class CreditCardControllerTest {
    @Mock
    private CreditCardService creditCardService;
    @InjectMocks
    private CreditCardController creditCardController;
    private CreditCard creditCard;
    @BeforeEach
    void setUp() {
        creditCard = new CreditCard("1", "1234-5678-9012-3456",
            CreditCardType.PERSONAL_CREDIT_CARD,
            new BigDecimal(10000),
            new BigDecimal(10000),
            "ACTIVE",
            LocalDateTime.now(),
            LocalDateTime.now(),
            PaymentStatus.PENDING,
            LocalDateTime.now(),
            LocalDateTime.now(),
            new BigDecimal("10.00")
        );
    }
    @Test
    void testCreateCreditCard() {
        when(creditCardService.createCreditCard(any(CreditCard.class)))
                .thenReturn(Mono.just(creditCard));
        StepVerifier.create(creditCardController.createCreditCard(creditCard))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.CREATED &&
                        response.getBody().getData().equals(creditCard))
                .verifyComplete();
    }
    @Test
    void testGetAllCreditCards() {
        when(creditCardService.getAllCreditCards()).thenReturn(Flux.just(creditCard));
        StepVerifier.create(creditCardController.getAllCreditCards())
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK &&
                        response.getBody().getData().size() == 1)
                .verifyComplete();
    }
    @Test
    void testGetCreditCardById() {
        when(creditCardService.getCreditCardById("1")).thenReturn(Mono.just(creditCard));
        StepVerifier.create(creditCardController.getCreditCardById("1"))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK &&
                        response.getBody().getData().equals(creditCard))
                .verifyComplete();
    }
    @Test
    void testGetCreditCardByCustomerId() {
        when(creditCardService.getCreditCardsByCustomerId("1")).thenReturn(Flux.just(creditCard));
        StepVerifier.create(creditCardController.getCreditCardsByCustomerId("1"))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK &&
                        response.getBody().getData().size() == 1)
                .verifyComplete();
    }
    @Test
    void testUpdateCreditCard() {
        when(creditCardService.updateCreditCard(eq("1"), any(CreditCard.class)))
                .thenReturn(Mono.just(creditCard));
        StepVerifier.create(creditCardController.updateCreditCard("1", creditCard))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK &&
                        response.getBody().getData().equals(creditCard))
                .verifyComplete();
    }
    @Test
    void testDeleteCreditCard() {
        when(creditCardService.deleteCreditCard("1")).thenReturn(Mono.empty());
        StepVerifier.create(creditCardController.deleteCreditCard("1"))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK &&
                        response.getBody().getMessage().equals("Credit Card deleted successfully"))
                .verifyComplete();
    }
}
