package com.bank.creditservice.model.credit;

import com.bank.creditservice.model.creditcard.PaymentStatus;
import lombok.*;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "credits")
public class Credit {
    @Id
    private String id;
    private String customerId;
    private CreditType creditType;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private BigDecimal interestRate;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    private PaymentStatus paymentStatus;
    private CreditStatus creditStatus;
    private LocalDateTime nextPaymentDate;
    private BigDecimal minimumPayment;
}
