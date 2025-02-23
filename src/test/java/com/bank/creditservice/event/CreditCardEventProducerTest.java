package com.bank.creditservice.event;

import com.bank.creditservice.model.creditcard.CreditCard;
import com.bank.creditservice.model.creditcard.CreditCardType;
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
class CreditCardEventProducerTest {
    @Mock
    private KafkaTemplate<String, CreditCard> kafkaTemplate;
    private CreditCardEventProducer creditCardEventProducer;
    @BeforeEach
    void setUp() {
        creditCardEventProducer = new CreditCardEventProducer(kafkaTemplate);
    }
    @Test
    void publishCreditCardCreated_Success() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        ListenableFuture<SendResult<String, CreditCard>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("creditcard-created", creditCard.getId(), creditCard))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, CreditCard>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            successCallback.onSuccess(mock(SendResult.class));
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditCardEventProducer.publishCreditCardCreated(creditCard);
        // Assert
        verify(kafkaTemplate).send("creditcard-created", creditCard.getId(), creditCard);
    }
    @Test
    void publishCreditCardCreated_Error() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        RuntimeException exception = new RuntimeException("Error sending message");
        ListenableFuture<SendResult<String, CreditCard>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("creditcard-created", creditCard.getId(), creditCard))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, CreditCard>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            failureCallback.onFailure(exception);
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditCardEventProducer.publishCreditCardCreated(creditCard);
        // Assert
        verify(kafkaTemplate).send("creditcard-created", creditCard.getId(), creditCard);
    }
    @Test
    void publishCreditCardUpdated_Success() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        ListenableFuture<SendResult<String, CreditCard>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("creditcard-updated", creditCard.getId(), creditCard))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, CreditCard>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            successCallback.onSuccess(mock(SendResult.class));
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditCardEventProducer.publishCreditCardUpdated(creditCard);
        // Assert
        verify(kafkaTemplate).send("creditcard-updated", creditCard.getId(), creditCard);
    }
    @Test
    void publishCreditCardUpdated_Error() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        RuntimeException exception = new RuntimeException("Error sending message");
        ListenableFuture<SendResult<String, CreditCard>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("creditcard-updated", creditCard.getId(), creditCard))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, CreditCard>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            failureCallback.onFailure(exception);
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        creditCardEventProducer.publishCreditCardUpdated(creditCard);
        // Assert
        verify(kafkaTemplate).send("creditcard-updated", creditCard.getId(), creditCard);
    }
    private CreditCard createCreditCard(String id) {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(id);
        creditCard.setCreditLimit(new BigDecimal("5000"));
        creditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        return creditCard;
    }
}
