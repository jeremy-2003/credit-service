package com.bank.creditservice.service;

import com.bank.creditservice.model.Credit;
import com.bank.creditservice.model.CreditCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class CreditEventProducer {
    private final KafkaTemplate<String, Credit> kafkaTemplate;
    public CreditEventProducer(KafkaTemplate<String, Credit> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void publishCreditCreated(Credit credit) {
        kafkaTemplate.send("credit-created", credit.getId(), credit)
                    .addCallback(
                            result -> log.info("Credit created event sent successfully: {}", credit.getId()),
                            ex -> log.error("Failed to send credit created event", ex));
    }
    public void publishCreditUpdated(Credit credit) {
        kafkaTemplate.send("credit-updated", credit.getId(), credit)
                    .addCallback(
                            result -> log.info("Credit updated event sent successfully: {}", credit.getId()),
                            ex -> log.error("Failed to send credit updated event", ex)
                    );
    }
}