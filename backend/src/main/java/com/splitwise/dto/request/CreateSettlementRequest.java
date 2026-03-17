package com.splitwise.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSettlementRequest {

    private Long paidByUserId;

    @NotNull(message = "Recipient user ID is required")
    private Long paidToUserId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private Long groupId;

    private LocalDate date;

    private String notes;
}
