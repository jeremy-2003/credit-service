package com.bank.creditservice.controller;
import com.bank.creditservice.model.credit.Credit;
import com.bank.creditservice.model.credit.CreditType;
import com.bank.creditservice.service.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditControllerTest {
    @Mock
    private CreditService creditService;
    @InjectMocks
    private CreditController creditController;
    private Credit testCredit;
    private WebTestClient webTestClient;
    @BeforeEach
    void setUp() {
        testCredit = createTestCredit();
        webTestClient = WebTestClient.bindToController(creditController).build();
    }
    private Credit createTestCredit() {
        Credit credit = new Credit();
        credit.setId("1");
        credit.setCustomerId("customer1");
        credit.setCreditType(CreditType.PERSONAL);
        credit.setAmount(new BigDecimal("1000.00"));
        credit.setRemainingBalance(new BigDecimal("1000.00"));
        credit.setInterestRate(new BigDecimal("0.05"));
        return credit;
    }
    @Test
    void createCredit_Success() {
        when(creditService.createCredit(any(Credit.class)))
                .thenReturn(Mono.just(testCredit));
        webTestClient.post()
                .uri("/api/credits")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCredit)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.CREATED.value())
                .jsonPath("$.message").isEqualTo("Credit created successfully")
                .jsonPath("$.data.id").isEqualTo(testCredit.getId());
    }
    @Test
    void createCredit_Error() {
        when(creditService.createCredit(any(Credit.class)))
                .thenReturn(Mono.error(new RuntimeException("Error creating credit")));
        webTestClient.post()
                .uri("/api/credits")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCredit)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.message").isEqualTo("Error creating credit")
                .jsonPath("$.data").isEqualTo(null);
    }
    @Test
    void getAllCredits_Success() {
        List<Credit> credits = Arrays.asList(testCredit);
        when(creditService.getAllCredits())
                .thenReturn(Flux.fromIterable(credits));
        webTestClient.get()
                .uri("/api/credits")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Credits retrieved successfully")
                .jsonPath("$.data[0].id").isEqualTo(testCredit.getId());
    }
    @Test
    void getAllCredits_Empty() {
        when(creditService.getAllCredits())
                .thenReturn(Flux.empty());
        webTestClient.get()
                .uri("/api/credits")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo("No credits found")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data").isEmpty();
    }
    @Test
    void getCreditById_Success() {
        when(creditService.getCreditById("1"))
                .thenReturn(Mono.just(testCredit));
        webTestClient.get()
                .uri("/api/credits/{creditId}", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Credit details retrieved successfully")
                .jsonPath("$.data.id").isEqualTo(testCredit.getId());
    }
    @Test
    void getCreditById_NotFound() {
        when(creditService.getCreditById("nonexistent"))
                .thenReturn(Mono.empty());
        webTestClient.get()
                .uri("/api/credits/{creditId}", "nonexistent")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo("Credit details retrieved not found")
                .jsonPath("$.data").isEqualTo(null);
    }
    @Test
    void getCreditById_Error() {
        when(creditService.getCreditById("1"))
                .thenReturn(Mono.error(new RuntimeException("Error getting credit")));
        webTestClient.get()
                .uri("/api/credits/{creditId}", "1")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.message").isEqualTo("Error getting credit")
                .jsonPath("$.data").isEqualTo(null);
    }
    @Test
    void getCreditsByCustomerId_Success() {
        List<Credit> credits = Arrays.asList(testCredit);
        when(creditService.getCreditsByCustomerId("customer1"))
                .thenReturn(Flux.fromIterable(credits));
        webTestClient.get()
                .uri("/api/credits/customer/{customerId}", "customer1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Credits for customer retrieved successfully")
                .jsonPath("$.data[0].id").isEqualTo(testCredit.getId());
    }
    @Test
    void getCreditsByCustomerId_NotFound() {
        when(creditService.getCreditsByCustomerId("nonexistent"))
                .thenReturn(Flux.error(new RuntimeException("This customer doesnt have credits")));
        webTestClient.get()
                .uri("/api/credits/customer/{customerId}", "nonexistent")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.message").isEqualTo("This customer doesnt have credits")
                .jsonPath("$.data").isEqualTo(null);
    }
    @Test
    void updateCredit_Success() {
        when(creditService.updateCredit(eq("1"), any(Credit.class)))
                .thenReturn(Mono.just(testCredit));
        webTestClient.put()
                .uri("/api/credits/{creditId}", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCredit)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Credit updated successfully")
                .jsonPath("$.data.id").isEqualTo(testCredit.getId());
    }
    @Test
    void updateCredit_NotFound() {
        when(creditService.updateCredit(eq("nonexistent"), any(Credit.class)))
                .thenReturn(Mono.empty());
        webTestClient.put()
                .uri("/api/credits/{creditId}", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCredit)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo("Credit not found")
                .jsonPath("$.data").isEqualTo(null);
    }
    @Test
    void deleteCredit_Success() {
        when(creditService.deleteCredit("1"))
                .thenReturn(Mono.empty());
        webTestClient.delete()
                .uri("/api/credits/{creditId}", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Credit deleted successfully")
                .jsonPath("$.data").isEqualTo(null);
    }
    @Test
    void deleteCredit_Error() {
        when(creditService.deleteCredit("1"))
                .thenReturn(Mono.error(new RuntimeException("Error deleting credit")));
        webTestClient.delete()
                .uri("/api/credits/{creditId}", "1")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.message").isEqualTo("Error deleting credit")
                .jsonPath("$.data").isEqualTo(null);
    }
}