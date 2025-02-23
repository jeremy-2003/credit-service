package com.bank.creditservice.service;
import com.bank.creditservice.model.customer.Customer;
import com.bank.creditservice.model.customer.CustomerType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.concurrent.TimeoutException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CustomerCacheServiceTest {
    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;
    private CustomerCacheService customerCacheService;
    private ObjectMapper objectMapper;
    private Customer testCustomer;
    @BeforeEach
    void setUp() {
        customerCacheService = new CustomerCacheService(redisTemplate);
        testCustomer = new Customer();
        testCustomer.setId("1");
        testCustomer.setFullName("Test Customer");
        testCustomer.setCustomerType(CustomerType.PERSONAL);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    @Test
    void saveCustomer_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String expectedJson = objectMapper.writeValueAsString(testCustomer);
        String expectedKey = "Customer:1";
        when(valueOperations.set(anyString(), anyString()))
                .thenReturn(Mono.just(Boolean.TRUE));
        StepVerifier.create(customerCacheService.saveCustomer(testCustomer.getId(), testCustomer))
                .verifyComplete();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture());
        assert keyCaptor.getValue().equals(expectedKey);
        assert objectMapper.readTree(valueCaptor.getValue())
                .equals(objectMapper.readTree(expectedJson));
    }
    @Test
    void saveCustomer_NullId_ReturnsError() {
        StepVerifier.create(customerCacheService.saveCustomer(null, testCustomer))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
    @Test
    void getCustomer_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String customerJson = objectMapper.writeValueAsString(testCustomer);
        String expectedKey = "Customer:1";
        when(valueOperations.get(eq(expectedKey)))
                .thenReturn(Mono.just(customerJson));
        StepVerifier.create(customerCacheService.getCustomer("1"))
                .expectNextMatches(customer ->
                        customer.getId().equals(testCustomer.getId()) &&
                                customer.getFullName().equals(testCustomer.getFullName()) &&
                                customer.getCustomerType() == testCustomer.getCustomerType())
                .verifyComplete();
    }
    @Test
    void getCustomer_NullId_ReturnsError() {
        StepVerifier.create(customerCacheService.getCustomer(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
    @Test
    void getCustomer_NotFound_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.empty());
        StepVerifier.create(customerCacheService.getCustomer("1"))
                .verifyComplete();
    }
    @Test
    void getCustomer_InvalidJson_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.just("invalid json"));
        StepVerifier.create(customerCacheService.getCustomer("1"))
                .verifyComplete();
    }
    @Test
    void getCustomer_Timeout_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.<String>never());
        StepVerifier.create(customerCacheService.getCustomer("1"))
                .verifyComplete();
    }
    @Test
    void getCustomer_RedisError_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis connection error")));
        StepVerifier.create(customerCacheService.getCustomer("1"))
                .verifyComplete();
    }
    @Test
    void saveCustomer_RedisError_PropagatesError() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis error")));
        StepVerifier.create(customerCacheService.saveCustomer("1", testCustomer))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void getCustomer_TimeoutException_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.error(new TimeoutException("Operation timed out")));
        StepVerifier.create(customerCacheService.getCustomer("1"))
                .verifyComplete();
    }
}