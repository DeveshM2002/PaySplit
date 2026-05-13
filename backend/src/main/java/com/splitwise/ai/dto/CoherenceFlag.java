package com.splitwise.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoherenceFlag {
    private String flagType;
    private Long expenseId1;
    private String expense1Description;
    private Long expenseId2;
    private String expense2Description;
    private String reason;
}
