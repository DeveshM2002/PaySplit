package com.splitwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents the balance between the current user and others.
 * Positive amount = they owe you. Negative = you owe them.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    private BigDecimal totalOwed;
    private BigDecimal totalOwing;
    private BigDecimal netBalance;
    private List<UserBalance> balances;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBalance {
        private UserResponse user;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DebtEntry {
        private UserResponse from;
        private UserResponse to;
        private BigDecimal amount;
    }
}
