package com.bank.creditservice.controller;

import com.bank.creditservice.dto.BaseResponse;
import com.bank.creditservice.model.credit.Credit;
import com.bank.creditservice.service.CreditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
public class CreditController {
    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @PostMapping
    public Mono<ResponseEntity<BaseResponse<Credit>>> createCredit(@RequestBody Credit credit) {
        return creditService.createCredit(credit)
                .map(savedCredit -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(BaseResponse.<Credit>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Credit created successfully")
                                .data(savedCredit)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<Credit>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @GetMapping
    public Mono<ResponseEntity<BaseResponse<List<Credit>>>> getAllCredits() {
        return creditService.getAllCredits()
                .collectList()
                .map(credits -> {
                    if (credits.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                               .body(BaseResponse.<List<Credit>>builder()
                                       .status(HttpStatus.NOT_FOUND.value())
                                       .message("No credits found")
                                       .data(credits)
                                       .build());
                    } else {
                        return ResponseEntity.ok(
                               BaseResponse.<List<Credit>>builder()
                                       .status(HttpStatus.OK.value())
                                       .message("Credits retrieved successfully")
                                       .data(credits)
                                       .build());
                    }
                });
    }
    @GetMapping("/{creditId}")
    public Mono<ResponseEntity<BaseResponse<Credit>>> getCreditById(@PathVariable String creditId) {
        return creditService.getCreditById(creditId)
                .map(credit -> ResponseEntity.ok(
                        BaseResponse.<Credit>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credit details retrieved successfully")
                                .data(credit)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<Credit>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Credit details retrieved not found")
                                .data(null)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<Credit>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @GetMapping("/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<List<Credit>>>> getCreditsByCustomerId(@PathVariable String customerId) {
        return creditService.getCreditsByCustomerId(customerId)
                .collectList()
                .map(credits -> ResponseEntity.ok(
                        BaseResponse.<List<Credit>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credits for customer retrieved successfully")
                                .data(credits)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<List<Credit>>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Credit for customer retrieved not found")
                                .data(null)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<List<Credit>>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @PutMapping("/{creditId}")
    public Mono<ResponseEntity<BaseResponse<Credit>>> updateCredit(@PathVariable String creditId,
                                                                   @RequestBody Credit updatedCredit) {
        return creditService.updateCredit(creditId, updatedCredit)
                .map(credit -> ResponseEntity.ok(
                        BaseResponse.<Credit>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credit updated successfully")
                                .data(credit)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<Credit>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Credit not found")
                                .data(null)
                                .build()
                        ))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.badRequest()
                            .body(BaseResponse.<Credit>builder()
                                    .status(HttpStatus.BAD_REQUEST.value())
                                    .message(e.getMessage())
                                    .data(null)
                                    .build()));
                });
    }
    @DeleteMapping("/{creditId}")
    public Mono<ResponseEntity<BaseResponse<Void>>> deleteCredit(@PathVariable String creditId) {
        return creditService.deleteCredit(creditId)
                .then(Mono.just(ResponseEntity.ok(
                        BaseResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Credit deleted successfully")
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