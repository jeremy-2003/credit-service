package com.bank.creditservice.service;

import com.bank.creditservice.event.CreditCardEventProducer;
import com.bank.creditservice.model.account.Account;
import com.bank.creditservice.model.account.AccountType;
import com.bank.creditservice.model.creditcard.CreditCard;
import com.bank.creditservice.model.creditcard.CreditCardType;
import com.bank.creditservice.model.customer.Customer;
import com.bank.creditservice.model.customer.CustomerType;
import com.bank.creditservice.repository.CreditCardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j

@Service
public class CreditCardService {
    private final CreditCardRepository creditCardRepository;
    private final CustomerCacheService customerCacheService;
    private final CustomerClientService customerClientService;
    private final CreditCardEventProducer creditCardEventProducer;
    private final AccountClientService accountClientService;
    public CreditCardService(CreditCardRepository creditCardRepository,
                             CustomerClientService customerClientService,
                             CustomerCacheService customerCacheService,
                             CreditCardEventProducer creditCardEventProducer,
                             AccountClientService accountClientService) {
        this.creditCardRepository = creditCardRepository;
        this.customerCacheService = customerCacheService;
        this.customerClientService = customerClientService;
        this.creditCardEventProducer = creditCardEventProducer;
        this.accountClientService = accountClientService;
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
    public Mono<CreditCard> createCreditCard(CreditCard creditCard) {
        return validateCustomer(creditCard.getCustomerId())
                .flatMap(validateCustomer -> {
                    if ((validateCustomer.getCustomerType() == CustomerType.PERSONAL &&
                            CreditCardType.BUSINESS_CREDIT_CARD == creditCard.getCardType()) ||
                            (validateCustomer.getCustomerType() == CustomerType.BUSINESS &&
                                    CreditCardType.PERSONAL_CREDIT_CARD == creditCard.getCardType())) {
                        return Mono.error(new RuntimeException("Customer type does not match credit card type"));
                    }
                    return accountClientService.getAccountsByCustomer(validateCustomer.getId())
                            .flatMapMany(accounts -> {
                                if (validateCustomer.getCustomerType() == CustomerType.PERSONAL
                                        && !accounts.isEmpty()) {
                                    boolean hasSavings = accounts.stream()
                                            .anyMatch(account -> AccountType.SAVINGS.equals(account.getAccountType()));
                                    if (hasSavings) {
                                        String accountId = accounts.stream()
                                                .filter(account -> AccountType.SAVINGS.equals(account.getAccountType()))
                                                .findFirst()
                                                .map(Account::getId)
                                                .orElse(null);
                                        if (accountId != null) {
                                            return accountClientService.updateVipPymStatus(accountId, true, "VIP")
                                                    .thenMany(Flux.defer(() -> customerClientService
                                                        .updateVipPymStatus(creditCard.getCustomerId(), true)
                                                            .thenMany(Flux.empty())));
                                        }
                                    }
                                } else if (validateCustomer.getCustomerType() == CustomerType.BUSINESS
                                        && !accounts.isEmpty()) {
                                    List<Mono<Void>> updateMonos = accounts.stream()
                                            .filter(account -> AccountType.CHECKING
                                                .equals(account.getAccountType()))
                                            .map(account -> accountClientService
                                                .updateVipPymStatus(account.getId(), true, "PYM").then())
                                            .collect(Collectors.toList());
                                    return Flux.concat(updateMonos)
                                            .thenMany(Flux.defer(() -> customerClientService
                                                .updateVipPymStatus(creditCard.getCustomerId(),
                                                        true).thenMany(Flux.empty())));
                                }
                                return Flux.empty();
                            })
                            .then(Mono.defer(() -> {
                                creditCard.setCreatedAt(LocalDateTime.now());
                                creditCard.setAvailableBalance(creditCard.getCreditLimit());
                                creditCard.setModifiedAt(null);
                                creditCard.setStatus("ACTIVE");
                                return creditCardRepository.save(creditCard);
                            }));
                })
                .doOnSuccess(creditCardEventProducer::publishCreditCardCreated);
    }

    public Flux<CreditCard> getAllCreditCards() {
        return creditCardRepository.findAll();
    }
    public Mono<CreditCard> getCreditCardById(String creditCardId) {
        return creditCardRepository.findById(creditCardId)
                .switchIfEmpty(Mono.error(new RuntimeException("This credit card doesn exist")));
    }
    public Flux<CreditCard> getCreditCardsByCustomerId(String customerId) {
        return creditCardRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new RuntimeException("This customer doesnt have credit cards")));
    }
    public Mono<Void> deleteCreditCard(String creditCardId) {
        AtomicReference<String> customerIdRef = new AtomicReference<>();
        AtomicReference<CustomerType> customerTypeRef = new AtomicReference<>();

        return creditCardRepository.findById(creditCardId)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit card not found")))
                .flatMap(existingCredit -> {
                    customerIdRef.set(existingCredit.getCustomerId());
                    return validateCustomer(existingCredit.getCustomerId())
                            .flatMap(validateCustomer -> {
                                customerTypeRef.set(validateCustomer.getCustomerType());
                                return creditCardRepository.deleteById(creditCardId)
                                        .thenMany(creditCardRepository.findByCustomerId(existingCredit.getCustomerId()))
                                        .collectList();
                            });
                })
                .flatMap(creditCards -> {
                    String customerId = customerIdRef.get();
                    CustomerType customerType = customerTypeRef.get();

                    if (creditCards.isEmpty()) {
                        Mono<Customer> updateCustomerVipPymStatus = customerClientService
                            .updateVipPymStatus(customerId, false);
                        if (customerType == CustomerType.PERSONAL) {
                            return accountClientService.getAccountsByCustomer(customerId)
                                    .flatMapMany(accounts -> {
                                        List<Mono<Void>> updateMonos = accounts.stream()
                                                .filter(account -> AccountType.SAVINGS
                                                    .equals(account.getAccountType()))
                                                .map(account -> accountClientService
                                                    .updateVipPymStatus(account.getId(), false, "VIP").then())
                                                .collect(Collectors.toList());
                                        return Flux.concat(updateMonos);
                                    })
                                    .then(updateCustomerVipPymStatus)
                                    .then(Mono.empty());
                        } else if (customerType == CustomerType.BUSINESS) {
                            return accountClientService.getAccountsByCustomer(customerId)
                                    .flatMapMany(accounts -> {
                                        List<Mono<Void>> updateMonos = accounts.stream()
                                            .filter(account -> AccountType.CHECKING
                                                .equals(account.getAccountType()))
                                            .map(account -> accountClientService
                                                .updateVipPymStatus(account.getId(), false, "PYM").then())
                                            .collect(Collectors.toList());
                                        return Flux.concat(updateMonos);
                                    })
                                    .then(updateCustomerVipPymStatus)
                                    .then(Mono.empty());
                        }
                    } else {
                        log.info("There are other credit cards for the customer.");
                    }
                    return Mono.empty();
                })
                .then();
    }
    public Mono<CreditCard> updateCreditCard(String creditCardId, CreditCard updatedCreditCard) {
        return creditCardRepository.findById(creditCardId)
                .flatMap(existingcredit -> {
                    existingcredit.setCreditLimit(updatedCreditCard.getCreditLimit());
                    existingcredit.setAvailableBalance(updatedCreditCard.getAvailableBalance());
                    existingcredit.setStatus(updatedCreditCard.getStatus());
                    existingcredit.setModifiedAt(LocalDateTime.now());
                    return creditCardRepository.save(existingcredit);
                })
                .doOnSuccess(creditCardEventProducer::publishCreditCardUpdated);
    }
}
