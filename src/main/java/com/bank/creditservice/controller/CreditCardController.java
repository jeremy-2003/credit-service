package com.bank.creditservice.controller;

import com.bank.creditservice.dto.BaseResponse;
import com.bank.creditservice.model.creditcard.CreditCard;
import com.bank.creditservice.service.CreditCardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {
    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }
    @PostMapping
    public Mono<ResponseEntity<BaseResponse<CreditCard>>> createCreditCard(@RequestBody CreditCard creditCard) {
        return creditCardService.createCreditCard(creditCard)
                .map(savedCreditCard -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(BaseResponse.<CreditCard>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Credit Card created successfully")
                                .data(savedCreditCard)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<CreditCard>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @GetMapping
    public Mono<ResponseEntity<BaseResponse<List<CreditCard>>>> getAllCreditCards() {
        return creditCardService.getAllCreditCards()
                .collectList()
                .map(creditCards -> {
                    if (creditCards.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(BaseResponse.<List<CreditCard>>builder()
                                        .status(HttpStatus.NOT_FOUND.value())
                                        .message("No credit cards found")
                                        .data(creditCards)
                                        .build());
                    } else {
                        return ResponseEntity.ok(
                                BaseResponse.<List<CreditCard>>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Credit retrieved successfully")
                                        .data(creditCards)
                                        .build());
                    }
                });
    }
    @GetMapping("/{creditCardId}")
    public Mono<ResponseEntity<BaseResponse<CreditCard>>> getCreditCardById(@PathVariable String creditCardId) {
        return creditCardService.getCreditCardById(creditCardId)
                .map(creditCard -> ResponseEntity.ok(
                        BaseResponse.<CreditCard>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credit Card details retrieved successfully")
                                .data(creditCard)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<CreditCard>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Credit cards details retrieved not found")
                                .data(null)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<CreditCard>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @GetMapping("/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<List<CreditCard>>>> getCreditCardsByCustomerId(
            @PathVariable String customerId) {
        return creditCardService.getCreditCardsByCustomerId(customerId)
                .collectList()
                .map(creditCards -> ResponseEntity.ok(
                        BaseResponse.<List<CreditCard>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credit Cards for customer retrieved successfully")
                                .data(creditCards)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<List<CreditCard>>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Credit cards for customer retrieved not found")
                                .data(null)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<List<CreditCard>>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @PutMapping("/{creditCardId}")
    public Mono<ResponseEntity<BaseResponse<CreditCard>>> updateCreditCard(@PathVariable String creditCardId,
                                                                           @RequestBody CreditCard updatedCreditCard) {
        return creditCardService.updateCreditCard(creditCardId, updatedCreditCard)
                .map(creditCard -> ResponseEntity.ok(
                        BaseResponse.<CreditCard>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credit Card updated successfully")
                                .data(creditCard)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<CreditCard>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Credit Card not found")
                                .data(null)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<CreditCard>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @DeleteMapping("/{creditCardId}")
    public Mono<ResponseEntity<BaseResponse<Void>>> deleteCreditCard(@PathVariable String creditCardId) {
        return creditCardService.deleteCreditCard(creditCardId)
                .then(Mono.just(ResponseEntity.ok(
                        BaseResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credit Card deleted successfully")
                                .data(null)
                                .build()
                )))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<Void>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
}