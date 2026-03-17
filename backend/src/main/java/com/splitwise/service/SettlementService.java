package com.splitwise.service;

import com.splitwise.dto.request.CreateSettlementRequest;
import com.splitwise.dto.response.SettlementResponse;
import com.splitwise.exception.BadRequestException;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.mapper.EntityMapper;
import com.splitwise.model.Group;
import com.splitwise.model.Settlement;
import com.splitwise.model.User;
import com.splitwise.model.enums.ActivityType;
import com.splitwise.repository.SettlementRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final ActivityService activityService;
    private final EntityMapper entityMapper;

    @Transactional
    public SettlementResponse createSettlement(Long paidByUserId, CreateSettlementRequest request) {
        if (paidByUserId.equals(request.getPaidToUserId())) {
            throw new BadRequestException("Cannot settle with yourself");
        }

        User paidBy = userRepository.findById(paidByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", paidByUserId));
        User paidTo = userRepository.findById(request.getPaidToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getPaidToUserId()));

        Group group = null;
        if (request.getGroupId() != null) {
            group = groupService.findGroupOrThrow(request.getGroupId());
        }

        Settlement settlement = Settlement.builder()
                .paidBy(paidBy)
                .paidTo(paidTo)
                .amount(request.getAmount())
                .group(group)
                .date(request.getDate() != null ? request.getDate() : LocalDate.now())
                .notes(request.getNotes())
                .build();

        settlement = settlementRepository.save(settlement);

        activityService.logActivity(
                ActivityType.SETTLEMENT_ADDED,
                paidBy.getName() + " paid " + paidTo.getName() + " " + settlement.getAmount(),
                paidBy, group, settlement.getId(), "SETTLEMENT"
        );

        return entityMapper.toSettlementResponse(settlement);
    }

    public List<SettlementResponse> getGroupSettlements(Long groupId) {
        return settlementRepository.findByGroupIdOrderByDateDesc(groupId).stream()
                .map(entityMapper::toSettlementResponse)
                .collect(Collectors.toList());
    }

    public List<SettlementResponse> getUserSettlements(Long userId) {
        return settlementRepository.findSettlementsInvolvingUser(userId).stream()
                .map(entityMapper::toSettlementResponse)
                .collect(Collectors.toList());
    }

    public List<SettlementResponse> getSettlementsBetweenUsers(Long userId1, Long userId2) {
        return settlementRepository.findSettlementsBetweenUsers(userId1, userId2).stream()
                .map(entityMapper::toSettlementResponse)
                .collect(Collectors.toList());
    }
}
