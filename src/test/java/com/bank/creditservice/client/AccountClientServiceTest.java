package com.bank.creditservice.client;

import com.bank.creditservice.model.account.Account;
import com.bank.creditservice.model.account.AccountType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AccountClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private CircuitBreaker circuitBreaker;
    private AccountClientService accountClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        // Mock para CircuitBreaker
        when(circuitBreakerRegistry.circuitBreaker("accountService")).thenReturn(circuitBreaker);
        when(circuitBreaker.getName()).thenReturn("accountService");
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        accountClientService = spy(new AccountClientService(webClientBuilder,
                "http://localhost:8081",
                circuitBreakerRegistry));
    }
    @Test
    void getAccountsByCustomer_Success() {
        // Arrange
        String customerId = "123";
        List<Account> expectedAccounts = Arrays.asList(
                createAccount("ACC001", customerId, AccountType.SAVINGS),
                createAccount("ACC002", customerId, AccountType.CHECKING)
        );
        doReturn(Mono.just(expectedAccounts))
                .when(accountClientService)
                .getAccountsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(accountClientService.getAccountsByCustomer(customerId))
                .expectNextMatches(accounts ->
                        accounts.size() == 2 &&
                                accounts.get(0).getId().equals("ACC001") &&
                                accounts.get(0).getCustomerId().equals(customerId) &&
                                accounts.get(1).getId().equals("ACC002") &&
                                accounts.get(1).getAccountType().equals(AccountType.CHECKING))
                .verifyComplete();
    }
    @Test
    void getAccountsByCustomer_EmptyList() {
        // Arrange
        String customerId = "nonexistent";
        doReturn(Mono.just(Collections.emptyList()))
                .when(accountClientService)
                .getAccountsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(accountClientService.getAccountsByCustomer(customerId))
                .expectNextMatches(accounts -> accounts.isEmpty())
                .verifyComplete();
    }
    @Test
    void getAccountsByCustomer_Error() {
        // Arrange
        String customerId = "123";
        RuntimeException expectedError = new RuntimeException(
                "Account service is unavailable for retrieving account information. " +
                        "Cannot continue with the operation.");
        doReturn(Mono.error(expectedError))
                .when(accountClientService)
                .getAccountsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(accountClientService.getAccountsByCustomer(customerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Account service is unavailable"))
                .verify();
    }
    @Test
    void updateVipPymStatus_Success() {
        // Arrange
        String accountId = "ACC001";
        boolean isVipPym = true;
        String type = "VIP";
        Account updatedAccount = createAccount(accountId, "123", AccountType.SAVINGS);
        doReturn(Mono.just(updatedAccount))
                .when(accountClientService)
                .updateVipPymStatus(accountId, isVipPym, type);
        // Act & Assert
        StepVerifier.create(accountClientService.updateVipPymStatus(accountId, isVipPym, type))
                .expectNextMatches(account ->
                        account.getId().equals(accountId) &&
                                account.getCustomerId().equals("123") &&
                                account.getAccountType().equals(AccountType.SAVINGS))
                .verifyComplete();
    }
    @Test
    void updateVipPymStatus_Error() {
        // Arrange
        String accountId = "ACC001";
        boolean isVipPym = true;
        String type = "VIP";
        RuntimeException expectedError = new RuntimeException(
                "Account service is not available to update VIP/PYM status. " +
                        "Cannot continue with account creation.");
        doReturn(Mono.error(expectedError))
                .when(accountClientService)
                .updateVipPymStatus(accountId, isVipPym, type);
        // Act & Assert
        StepVerifier.create(accountClientService.updateVipPymStatus(accountId, isVipPym, type))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Account service is not available"))
                .verify();
    }
    private Account createAccount(String id, String customerId, AccountType type) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId(customerId);
        account.setAccountType(type);
        return account;
    }
}