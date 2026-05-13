package com.splitwise.ai.dto;

import com.splitwise.model.enums.ExpenseCategory;
import com.splitwise.model.enums.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDraftResponse {
    private String narrative;
    private ExpenseDraft draft;
    private boolean needsConfirmation;
    private String confirmationMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseDraft {
        private String description;
        private BigDecimal amount;
        private SplitType splitType;
        private ExpenseCategory category;
        private String date;
        private Long groupId;
        private String groupName;
        private Long paidById;
        private String paidByName;
        private String currency;
        private List<SplitDetail> splits;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitDetail {
        private Long userId;
        private String userName;
        private BigDecimal amount;
    }
}
