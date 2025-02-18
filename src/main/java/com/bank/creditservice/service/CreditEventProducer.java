package com.bank.creditservice.service;

import com.bank.creditservice.model.Credit;
import com.bank.creditservice.model.CreditCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class CreditEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate; // Cambiamos a Object para soportar ambos tipos
    public CreditEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void publishCreditCreated(Credit credit) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("credit-created", credit.getId(), credit)
                    .addCallback(
                            result -> log.info("Credit created event sent successfully: {}", credit.getId()),
                            ex -> log.error("Failed to send credit created event", ex)
                    );
            return null;
        });
    }
    public void publishCreditUpdated(Credit credit) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("credit-updated", credit.getId(), credit)
                    .addCallback(
                            result -> log.info("Credit updated event sent successfully: {}", credit.getId()),
                            ex -> log.error("Failed to send credit updated event", ex)
                    );
            return null;
        });
    }
    public void publishCreditCardCreated(CreditCard creditCard) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("creditcard-created", creditCard.getId(), creditCard)
                    .addCallback(
                            result -> log.info("Credit Card created event sent successfully: {}", creditCard.getId()),
                            ex -> log.error("Failed to send credit card created event", ex)
                    );
            return null;
        });
    }
    public void publishCreditCardUpdated(CreditCard creditCard) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("creditcard-updated", creditCard.getId(), creditCard)
                    .addCallback(
                            result -> log.info("Credit Card updated event sent successfully: {}", creditCard.getId()),
                            ex -> log.error("Failed to send credit card updated event", ex)
                    );
            return null;
        });
    }
}