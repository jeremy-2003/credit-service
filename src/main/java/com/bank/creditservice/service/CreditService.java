package com.bank.creditservice.service;

import com.bank.creditservice.model.*;
import com.bank.creditservice.repository.CreditCardRepository;
import com.bank.creditservice.repository.CreditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CreditService {
    private final CreditRepository creditRepository;
    private final CustomerCacheService customerCacheService;
    private final CustomerClientService customerClientService;
    private final CreditEventProducer creditEventProducer;
    public CreditService(CreditRepository creditRepository,
                         CustomerClientService customerClientService,
                         CustomerCacheService customerCacheService,
                         CreditEventProducer creditEventProducer) {
        this.creditRepository = creditRepository;
        this.customerCacheService = customerCacheService;
        this.customerClientService = customerClientService;
        this.creditEventProducer = creditEventProducer;
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
    public Mono<Credit> createCredit(Credit credit) {
        return validateCustomer(credit.getCustomerId())
                .flatMap(customer -> {
                    if ((customer.getCustomerType() == CustomerType.PERSONAL && credit.getCreditType() == CreditType.BUSINESS) ||
                            (customer.getCustomerType() == CustomerType.BUSINESS && credit.getCreditType() == CreditType.PERSONAL)) {
                        return Mono.error(new RuntimeException("Customer type does not match credit type"));
                    }
                     if (customer.getCustomerType() == CustomerType.PERSONAL) {
                        return creditRepository.findByCustomerId(credit.getCustomerId())
                                .hasElements()
                                .flatMap(hasCredit -> {
                                    if (hasCredit) {
                                        return Mono.error(new RuntimeException("Personal customer can only have one active credit"));
                                    }
                                    credit.setRemainingBalance(credit.getAmount());
                                    credit.setCreatedAt(LocalDateTime.now());
                                    return creditRepository.save(credit);
                                });
                    }
                    credit.setRemainingBalance(credit.getAmount());
                    credit.setCreatedAt(LocalDateTime.now());
                    return creditRepository.save(credit);
                })
                .doOnSuccess(creditEventProducer::publishCreditCreated);
    }
    public Flux<Credit> getAllCredits(){
        return creditRepository.findAll();
    }
    public Flux<Credit> getCreditsByCustomerId(String customerId){
        return creditRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new RuntimeException("This customer doesnt have credits")));
    }
    public Mono<Credit> getCreditById(String creditId){
        return creditRepository.findById(creditId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found")));
    }
    public Mono<Credit> updateCredit(String creditId, Credit updatedCredit){
        return creditRepository.findById(creditId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found")))
                .flatMap(existingCredit ->{
                    existingCredit.setAmount(updatedCredit.getAmount());
                    existingCredit.setInterestRate(updatedCredit.getInterestRate());
                    existingCredit.setRemainingBalance(updatedCredit.getRemainingBalance());
                    existingCredit.setModifiedAt(LocalDateTime.now());
                    return creditRepository.save(existingCredit);
                })
                .doOnSuccess(creditEventProducer::publishCreditUpdated);
    }
    public Mono<Void> deleteCredit(String creditId){
        return creditRepository.findById(creditId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found")))
                .flatMap(existingCredit -> creditRepository.deleteById(creditId));
    }

}
