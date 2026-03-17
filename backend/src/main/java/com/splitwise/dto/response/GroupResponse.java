package com.splitwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private UserResponse createdBy;
    private List<UserResponse> members;
    private Boolean simplifyDebts;
    private BigDecimal myBalance;
    private LocalDateTime createdAt;
}
