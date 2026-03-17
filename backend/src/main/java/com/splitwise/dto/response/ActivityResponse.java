package com.splitwise.dto.response;

import com.splitwise.model.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResponse {
    private Long id;
    private ActivityType type;
    private String description;
    private UserResponse performedBy;
    private Long groupId;
    private Long relatedEntityId;
    private String relatedEntityType;
    private LocalDateTime createdAt;
}
