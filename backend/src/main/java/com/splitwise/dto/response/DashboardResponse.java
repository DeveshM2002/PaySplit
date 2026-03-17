package com.splitwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private BigDecimal totalOwed;
    private BigDecimal totalOwing;
    private BigDecimal netBalance;
    private List<FriendBalanceSummary> friendBalances;
    private List<GroupBalanceSummary> groupBalances;
    private List<ActivityResponse> recentActivity;
    private Map<String, BigDecimal> categorySpending;
    private Map<String, BigDecimal> monthlySpending;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FriendBalanceSummary {
        private Long friendId;
        private String friendName;
        private String friendEmail;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupBalanceSummary {
        private Long groupId;
        private String groupName;
        private BigDecimal balance;
    }
}
