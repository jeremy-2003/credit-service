package com.bank.creditservice.controller;

import com.bank.creditservice.dto.BaseResponse;
import com.bank.creditservice.service.CustomerEligibilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/customer-eligibility")
public class CustomerEligibilityController {
    @Autowired
    private CustomerEligibilityService customerEligibilityService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerEligibilityController.class);

    @GetMapping("/has-overdue-debt/{customerId}")
    public Mono<ResponseEntity<BaseResponse<Boolean>>> hasOverdueDebt(@PathVariable String customerId) {
        log.info("Received request to check if customer {} has overdue debt", customerId);
        return customerEligibilityService.hasOverdueDebt(customerId)
                .map(hasDebt -> {
                    BaseResponse<Boolean> response = new BaseResponse<>();
                    response.setData(hasDebt);
                    response.setStatus(HttpStatus.OK.value());
                    response.setMessage(hasDebt
                            ? "Customer has overdue debt and cannot acquire new products"
                            : "Customer has no overdue debt");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Error checking customer debt status: {}", e.getMessage());
                    BaseResponse<Boolean> errorResponse = new BaseResponse<>();
                    errorResponse.setData(true);
                    errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    errorResponse.setMessage("Error checking debt status. Assuming customer has debt.");
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResponse));
                });
    }

    @GetMapping("/is-eligible/{customerId}")
    public Mono<ResponseEntity<BaseResponse<Boolean>>> isEligibleForNewProduct(@PathVariable String customerId) {
        log.info("Received request to check if customer {} is eligible for new products", customerId);
        return customerEligibilityService.isCustomerEligibleForNewProduct(customerId)
                .map(isEligible -> {
                    BaseResponse<Boolean> response = new BaseResponse<>();
                    response.setData(isEligible);
                    response.setStatus(HttpStatus.OK.value());
                    response.setMessage(isEligible
                            ? "Customer is eligible to acquire new products"
                            : "Customer is not eligible due to existing overdue debt");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Error checking customer eligibility: {}", e.getMessage());
                    BaseResponse<Boolean> errorResponse = new BaseResponse<>();
                    errorResponse.setData(false);
                    errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    errorResponse.setMessage("Error checking eligibility. Assuming customer is not eligible.");
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResponse));
                });
    }
}
