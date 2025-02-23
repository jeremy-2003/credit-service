package com.bank.creditservice.event;

import com.bank.creditservice.model.credit.Credit;
import com.bank.creditservice.model.credit.CreditType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditEventProducerTest {
    @Mock
    private KafkaTemplate<String, Credit> kafkaTemplate;
    private CreditEventProducer creditEventProducer;
    @BeforeEach
    void setUp() {
        creditEventProducer = new CreditEventProducer(kafkaTemplate);
    }
    @Test
    void publishCreditCreated_Success() {
        // Arrange
        Credit credit = createCredit("123");
        ListenableFuture<SendResult<String, Credit>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("credit-created", credit.getId(), credit))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Credit>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            successCallback.onSuccess(mock(SendResult.class));
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditEventProducer.publishCreditCreated(credit);
        // Assert
        verify(kafkaTemplate).send("credit-created", credit.getId(), credit);
    }
    @Test
    void publishCreditCreated_Error() {
        // Arrange
        Credit credit = createCredit("123");
        RuntimeException exception = new RuntimeException("Error sending message");
        ListenableFuture<SendResult<String, Credit>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("credit-created", credit.getId(), credit))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Credit>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            failureCallback.onFailure(exception);
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditEventProducer.publishCreditCreated(credit);
        // Assert
        verify(kafkaTemplate).send("credit-created", credit.getId(), credit);
    }
    @Test
    void publishCreditUpdated_Success() {
        // Arrange
        Credit credit = createCredit("123");
        ListenableFuture<SendResult<String, Credit>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("credit-updated", credit.getId(), credit))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Credit>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            successCallback.onSuccess(mock(SendResult.class));
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditEventProducer.publishCreditUpdated(credit);
        // Assert
        verify(kafkaTemplate).send("credit-updated", credit.getId(), credit);
    }
    @Test
    void publishCreditUpdated_Error() {
        // Arrange
        Credit credit = createCredit("123");
        RuntimeException exception = new RuntimeException("Error sending message");
        ListenableFuture<SendResult<String, Credit>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("credit-updated", credit.getId(), credit))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Credit>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            failureCallback.onFailure(exception);
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditEventProducer.publishCreditUpdated(credit);
        // Assert
        verify(kafkaTemplate).send("credit-updated", credit.getId(), credit);
    }
    private Credit createCredit(String id) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setAmount(new BigDecimal("10000"));
        credit.setCreditType(CreditType.PERSONAL);
        return credit;
    }
}