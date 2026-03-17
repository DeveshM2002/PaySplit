package com.splitwise.dto.response;

import com.splitwise.model.enums.ExpenseCategory;
import com.splitwise.model.enums.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private String description;
    private BigDecimal amount;
    private SplitType splitType;
    private ExpenseCategory category;
    private LocalDate date;
    private UserResponse paidBy;
    private Long groupId;
    private String groupName;
    private List<ExpenseSplitResponse> splits;
    private Boolean isRecurring;
    private String currency;
    private int commentCount;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseSplitResponse {
        private Long id;
        private UserResponse user;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
