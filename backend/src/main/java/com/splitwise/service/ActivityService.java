package com.splitwise.service;

import com.splitwise.dto.response.ActivityResponse;
import com.splitwise.mapper.EntityMapper;
import com.splitwise.model.Activity;
import com.splitwise.model.Group;
import com.splitwise.model.User;
import com.splitwise.model.enums.ActivityType;
import com.splitwise.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final EntityMapper entityMapper;

    @Transactional
    public void logActivity(ActivityType type, String description, User performedBy,
                            Group group, Long relatedEntityId, String relatedEntityType) {
        Activity activity = Activity.builder()
                .type(type)
                .description(description)
                .performedBy(performedBy)
                .group(group)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();
        activityRepository.save(activity);
    }

    public List<ActivityResponse> getGroupActivities(Long groupId) {
        return activityRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(entityMapper::toActivityResponse)
                .collect(Collectors.toList());
    }

    public Page<ActivityResponse> getUserActivities(Long userId, int page, int size) {
        Page<Activity> activities = activityRepository.findActivitiesForUser(
                userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return activities.map(entityMapper::toActivityResponse);
    }
}
