package com.splitwise.model;

import com.splitwise.model.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Activity — an audit log / activity feed entry.
 *
 * Every significant action (expense added, settlement made, member joined)
 * generates an Activity record. This powers the "Recent Activity" feed.
 *
 * WHY a dedicated Activity entity instead of computing from other tables?
 * - Computing activity by joining expenses + settlements + groups is complex and slow
 * - Dedicated table is fast to query (single table scan) and easy to paginate
 * - We can add activities for events that don't map to any single entity (e.g., "Bob left the group")
 * - Trade-off: Slight data redundancy, but worth it for query simplicity
 *
 * This is the "Event Sourcing lite" pattern — we log events, not just state.
 */
@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    private Long relatedEntityId;

    private String relatedEntityType;
}
