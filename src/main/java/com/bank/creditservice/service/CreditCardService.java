package com.bank.creditservice.service;

import com.bank.creditservice.model.*;
import com.bank.creditservice.repository.CreditCardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j

@Service
public class CreditCardService {
    private final CreditCardRepository creditCardRepository;
    private final CustomerCacheService customerCacheService;
    private final CustomerClientService customerClientService;

    public CreditCardService(CreditCardRepository creditCardRepository,
                             CustomerClientService customerClientService,
                             CustomerCacheService customerCacheService){
        this.creditCardRepository = creditCardRepository;
        this.customerCacheService = customerCacheService;
        this.customerClientService = customerClientService;
    }
    private Mono<Customer> validateCustomer(String customerId) {
        log.info("Validating customer with ID: {}", customerId);
        return customerCacheService.getCustomer(customerId)
                .doOnNext(customer -> log.info("Customer found in cache: {}", customer.getId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Customer not found in cache, fetching from service: {}", customerId);
                    return fetchCustomerFromService(customerId);
                }))
                .doOnError(e -> log.error("Error in customer validation: {}", e.getMessage()))
                .onErrorResume(ex -> {
                    log.error("Final error handling in validateCustomer: {}", ex.getMessage());
                    return fetchCustomerFromService(customerId);
                });
    }
    private Mono<Customer> fetchCustomerFromService(String customerId) {
        return customerClientService.getCustomerById(customerId)
                .flatMap(customer -> {
                    try {
                        return customerCacheService.saveCustomer(customerId, customer)
                                .then(Mono.just(customer));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error serializing customer", e));
                    }
                })
                .onErrorResume(e -> Mono.empty());
    }
    public Mono<CreditCard> createCreditCard(CreditCard creditCard){
        return validateCustomer(creditCard.getCustomerId())
                .flatMap(validateCustomer->{
                    if ((validateCustomer.getCustomerType() == CustomerType.PERSONAL && CreditCardType.BUSINESS_CREDIT_CARD == creditCard.getCardType()) ||
                            (validateCustomer.getCustomerType() == CustomerType.BUSINESS && CreditCardType.PERSONAL_CREDIT_CARD == creditCard.getCardType())) {
                        return Mono.error(new RuntimeException("Customer type does not match credit card type"));
                    }
                    creditCard.setCreatedAt(LocalDateTime.now());
                    creditCard.setModifiedAt(null);
                    creditCard.setStatus("ACTIVE");
                    return creditCardRepository.save(creditCard);
                });

    }
    public Flux<CreditCard> getAllCreditCards(){
        return creditCardRepository.findAll();
    }
    public Mono<CreditCard> getCreditCardById(String creditCardId){
        return creditCardRepository.findById(creditCardId)
                .switchIfEmpty(Mono.error(new RuntimeException("This credit card doesn exist")));
    }
    public Flux<CreditCard> getCreditCardsByCustomerId(String customerId){
        return creditCardRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new RuntimeException("This customer doesnt have credit cards")));
    }
    public Mono<Void> deleteCreditCard(String creditCardId){
        return creditCardRepository.findById(creditCardId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit card not found")))
                .flatMap(existingCredit -> creditCardRepository.deleteById(creditCardId));
    }
    public Mono<CreditCard> updateCreditCard(String creditCardId, CreditCard updatedCreditCard){
        return creditCardRepository.findById(creditCardId)
                .flatMap(existingcredit -> {
                    existingcredit.setCreditLimit(updatedCreditCard.getCreditLimit());
                    existingcredit.setAvailableBalance(updatedCreditCard.getAvailableBalance());
                    existingcredit.setStatus(updatedCreditCard.getStatus());
                    existingcredit.setModifiedAt(LocalDateTime.now());
                    return creditCardRepository.save(existingcredit);
                });
    }
}
