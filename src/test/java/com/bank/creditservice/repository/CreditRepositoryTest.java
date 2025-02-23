package com.bank.creditservice.repository;
import com.bank.creditservice.model.credit.Credit;
import com.bank.creditservice.model.credit.CreditType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class CreditRepositoryTest {
    @Mock
    private CreditRepository creditRepository;
    private Credit testCredit1;
    private Credit testCredit2;
    @BeforeEach
    void setUp() {
        testCredit1 = createTestCredit("1", "customer1", CreditType.PERSONAL);
        testCredit2 = createTestCredit("2", "customer1", CreditType.BUSINESS);
    }
    private Credit createTestCredit(String id, String customerId, CreditType type) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setCustomerId(customerId);
        credit.setCreditType(type);
        credit.setAmount(new BigDecimal("1000.00"));
        credit.setRemainingBalance(new BigDecimal("1000.00"));
        credit.setInterestRate(new BigDecimal("0.05"));
        credit.setCreatedAt(LocalDateTime.now());
        return credit;
    }
    @Test
    void findByCustomerId_WhenCreditsExist_ReturnsCredits() {
        when(creditRepository.findByCustomerId("customer1"))
                .thenReturn(Flux.just(testCredit1, testCredit2));
        StepVerifier.create(creditRepository.findByCustomerId("customer1"))
                .expectNext(testCredit1)
                .expectNext(testCredit2)
                .verifyComplete();
    }
    @Test
    void findByCustomerId_WhenNoCredits_ReturnsEmptyFlux() {
        when(creditRepository.findByCustomerId("nonexistent"))
                .thenReturn(Flux.empty());
        StepVerifier.create(creditRepository.findByCustomerId("nonexistent"))
                .verifyComplete();
    }
    @Test
    void save_NewCredit_SavesSuccessfully() {
        when(creditRepository.save(any(Credit.class)))
                .thenReturn(Mono.just(testCredit1));
        StepVerifier.create(creditRepository.save(testCredit1))
                .expectNext(testCredit1)
                .verifyComplete();
    }
    @Test
    void findById_ExistingCredit_ReturnsCredit() {
        when(creditRepository.findById("1"))
                .thenReturn(Mono.just(testCredit1));
        StepVerifier.create(creditRepository.findById("1"))
                .expectNext(testCredit1)
                .verifyComplete();
    }
    @Test
    void findById_NonExistingCredit_ReturnsEmptyMono() {
        when(creditRepository.findById("nonexistent"))
                .thenReturn(Mono.empty());
        StepVerifier.create(creditRepository.findById("nonexistent"))
                .verifyComplete();
    }
    @Test
    void findAll_WhenCreditsExist_ReturnsAllCredits() {
        when(creditRepository.findAll())
                .thenReturn(Flux.just(testCredit1, testCredit2));
        StepVerifier.create(creditRepository.findAll())
                .expectNext(testCredit1)
                .expectNext(testCredit2)
                .verifyComplete();
    }
    @Test
    void findAll_WhenNoCredits_ReturnsEmptyFlux() {
        when(creditRepository.findAll())
                .thenReturn(Flux.empty());
        StepVerifier.create(creditRepository.findAll())
                .verifyComplete();
    }
    @Test
    void deleteById_DeletesSuccessfully() {
        when(creditRepository.deleteById(anyString()))
                .thenReturn(Mono.empty());
        StepVerifier.create(creditRepository.deleteById("1"))
                .verifyComplete();
    }
    @Test
    void existsById_WhenExists_ReturnsTrue() {
        when(creditRepository.existsById("1"))
                .thenReturn(Mono.just(true));
        StepVerifier.create(creditRepository.existsById("1"))
                .expectNext(true)
                .verifyComplete();
    }
    @Test
    void existsById_WhenNotExists_ReturnsFalse() {
        when(creditRepository.existsById("nonexistent"))
                .thenReturn(Mono.just(false));
        StepVerifier.create(creditRepository.existsById("nonexistent"))
                .expectNext(false)
                .verifyComplete();
    }
}