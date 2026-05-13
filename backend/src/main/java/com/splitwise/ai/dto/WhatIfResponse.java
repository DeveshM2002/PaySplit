package com.splitwise.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatIfResponse {
    private Map<String, BigDecimal> originalCategoryTotals;
    private Map<String, BigDecimal> adjustedCategoryTotals;
    private Map<String, BigDecimal> originalMonthlyTotals;
    private Map<String, BigDecimal> adjustedMonthlyTotals;
    private BigDecimal originalTotal;
    private BigDecimal adjustedTotal;
    private BigDecimal difference;
    private String narrative;
}
