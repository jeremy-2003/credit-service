package com.bank.creditservice.repository;

import com.bank.creditservice.model.CreditCard;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface CreditCardRepository extends ReactiveMongoRepository<CreditCard, String> {
    Flux<CreditCard> findByCustomerId(String customerId);
}
