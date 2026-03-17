package com.splitwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementResponse {
    private Long id;
    private UserResponse paidBy;
    private UserResponse paidTo;
    private BigDecimal amount;
    private Long groupId;
    private LocalDate date;
    private String notes;
    private LocalDateTime createdAt;
}
