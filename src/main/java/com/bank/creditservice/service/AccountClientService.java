package com.bank.creditservice.service;

import com.bank.creditservice.dto.BaseResponse;
import com.bank.creditservice.model.account.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AccountClientService {
    private final WebClient webClient;
    private final String accountServiceUrl;
    public AccountClientService(WebClient.Builder webClientBuilder,
                                @Value("${account-service.base-url}") String accountServiceUrl){
        this.accountServiceUrl = accountServiceUrl;
        this.webClient = webClientBuilder.baseUrl(accountServiceUrl).build();
    }
    public Mono<List<Account>> getAccountsByCustomer(String customerId) {
        String fullUrl = accountServiceUrl + "/customer/" + customerId;
        log.info("Sending request to Account Service API: {}", fullUrl);
        return webClient.get()
                .uri("/customer/{customerId}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.empty();
                    }
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Account>>>() {})
                .flatMap(response -> {
                    if (response.getData() != null) {
                        return Mono.just(response.getData());
                    } else {
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(Mono.just(Collections.emptyList()))
                .doOnNext(result -> log.info("Account API response: {}", result))
                .doOnError(e -> log.error("Error while fetching Accounts: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("Request to Account API completed"));
    }

    public Mono<Account> updateVipPymStatus(String accountId, boolean isVipPym, String type) {
        log.info("Sending PUT request to Account Service API for accountId: {}", accountId);
        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/{accountId}/vip-pym/status")
                        .queryParam("isVipPym", isVipPym)
                        .queryParam("type", type)
                        .build(accountId)
                )
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                )
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Account>>() {})
                .flatMap(response -> response.getData() != null ? Mono.just(response.getData()) : Mono.empty())
                .doOnNext(result -> log.info("Account API response: {}", result))
                .doOnError(e -> log.error("Error while updating account: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("PUT request to Account API completed"));
    }

}
