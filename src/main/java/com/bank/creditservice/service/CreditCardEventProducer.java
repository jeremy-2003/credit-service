package com.bank.creditservice.service;
import com.bank.creditservice.model.Credit;
import com.bank.creditservice.model.CreditCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class CreditCardEventProducer {
    private final KafkaTemplate<String, CreditCard> kafkaTemplate;
    public CreditCardEventProducer(KafkaTemplate<String, CreditCard> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void publishCreditCardCreated(CreditCard creditCard) {
        kafkaTemplate.send("creditcard-created", creditCard.getId(), creditCard)
                .addCallback(
                        result -> log.info("Credit Card created event sent successfully: {}", creditCard.getId()),
                        ex -> log.error("Failed to send credit card created event", ex)
                );
    }
    public void publishCreditCardUpdated(CreditCard creditCard) {
        kafkaTemplate.send("creditcard-updated", creditCard.getId(), creditCard)
                .addCallback(
                        result -> log.info("Credit Card updated event sent successfully: {}", creditCard.getId()),
                        ex -> log.error("Failed to send credit card updated event", ex)
                );
    }
}
