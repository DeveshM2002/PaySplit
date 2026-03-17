package com.splitwise.service;

import com.splitwise.dto.request.CreateGroupRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.dto.response.BalanceResponse;
import com.splitwise.dto.response.GroupResponse;
import com.splitwise.exception.BadRequestException;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.mapper.EntityMapper;
import com.splitwise.model.Group;
import com.splitwise.model.User;
import com.splitwise.model.enums.ActivityType;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final EntityMapper entityMapper;
    private final PasswordEncoder passwordEncoder;
    private final BalanceService balanceService;

    @Transactional
    public GroupResponse createGroup(Long creatorId, CreateGroupRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .createdBy(creator)
                .simplifyDebts(request.getSimplifyDebts() != null ? request.getSimplifyDebts() : true)
                .build();

        Set<User> members = new HashSet<>();
        members.add(creator);

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<User> additionalMembers = userRepository.findByIdIn(request.getMemberIds());
            members.addAll(additionalMembers);
        }

        if (request.getMemberNames() != null && !request.getMemberNames().isEmpty()) {
            for (String name : request.getMemberNames()) {
                if (name != null && !name.trim().isEmpty()) {
                    members.add(createGuestUser(name.trim()));
                }
            }
        }

        group.setMembers(members);
        for (User member : members) {
            member.getGroups().add(group);
        }

        group = groupRepository.save(group);

        activityService.logActivity(
                ActivityType.GROUP_CREATED,
                creator.getName() + " created group \"" + group.getName() + "\"",
                creator, group, group.getId(), "GROUP"
        );

        return entityMapper.toGroupResponse(group);
    }

    public GroupResponse getGroupById(Long groupId, Long userId) {
        Group group = findGroupOrThrow(groupId);
        validateMembership(group, userId);
        return entityMapper.toGroupResponse(group);
    }

    public List<GroupResponse> getUserGroups(Long userId) {
        return groupRepository.findGroupsByUserId(userId).stream()
                .map(group -> {
                    GroupResponse response = entityMapper.toGroupResponse(group);
                    BalanceResponse balance = balanceService.getGroupBalance(group.getId(), userId);
                    response.setMyBalance(balance.getNetBalance());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupResponse updateGroup(Long groupId, Long userId, CreateGroupRequest request) {
        Group group = findGroupOrThrow(groupId);
        validateMembership(group, userId);

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            group.setImageUrl(request.getImageUrl());
        }
        if (request.getSimplifyDebts() != null) {
            group.setSimplifyDebts(request.getSimplifyDebts());
        }

        group = groupRepository.save(group);

        User user = userRepository.findById(userId).orElseThrow();
        activityService.logActivity(
                ActivityType.GROUP_UPDATED,
                user.getName() + " updated group \"" + group.getName() + "\"",
                user, group, group.getId(), "GROUP"
        );

        return entityMapper.toGroupResponse(group);
    }

    @Transactional
    public GroupResponse addMember(Long groupId, Long userId, Long newMemberId) {
        Group group = findGroupOrThrow(groupId);
        validateMembership(group, userId);

        User newMember = userRepository.findById(newMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", newMemberId));

        if (group.getMembers().contains(newMember)) {
            throw new BadRequestException("User is already a member of this group");
        }

        group.getMembers().add(newMember);
        newMember.getGroups().add(group);
        group = groupRepository.save(group);

        User adder = userRepository.findById(userId).orElseThrow();
        activityService.logActivity(
                ActivityType.MEMBER_ADDED,
                adder.getName() + " added " + newMember.getName() + " to the group",
                adder, group, newMemberId, "USER"
        );

        return entityMapper.toGroupResponse(group);
    }

    @Transactional
    public GroupResponse removeMember(Long groupId, Long userId, Long memberToRemoveId) {
        Group group = findGroupOrThrow(groupId);
        validateMembership(group, userId);

        if (group.getCreatedBy().getId().equals(memberToRemoveId)) {
            throw new BadRequestException("Cannot remove the group creator");
        }

        User memberToRemove = userRepository.findById(memberToRemoveId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", memberToRemoveId));

        group.getMembers().remove(memberToRemove);
        memberToRemove.getGroups().remove(group);
        groupRepository.save(group);

        User remover = userRepository.findById(userId).orElseThrow();
        activityService.logActivity(
                ActivityType.MEMBER_REMOVED,
                remover.getName() + " removed " + memberToRemove.getName() + " from the group",
                remover, group, memberToRemoveId, "USER"
        );

        return entityMapper.toGroupResponse(group);
    }

    @Transactional
    public ApiResponse deleteGroup(Long groupId, Long userId) {
        Group group = findGroupOrThrow(groupId);

        if (!group.getCreatedBy().getId().equals(userId)) {
            throw new BadRequestException("Only the group creator can delete the group");
        }

        for (User member : new HashSet<>(group.getMembers())) {
            member.getGroups().remove(group);
        }
        group.getMembers().clear();

        groupRepository.delete(group);
        return new ApiResponse(true, "Group deleted successfully");
    }

    public Group findGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
    }

    @Transactional
    public GroupResponse addMemberByName(Long groupId, Long userId, String memberName) {
        Group group = findGroupOrThrow(groupId);
        validateMembership(group, userId);

        User newMember = createGuestUser(memberName.trim());

        group.getMembers().add(newMember);
        newMember.getGroups().add(group);
        group = groupRepository.save(group);

        User adder = userRepository.findById(userId).orElseThrow();
        activityService.logActivity(
                ActivityType.MEMBER_ADDED,
                adder.getName() + " added " + newMember.getName() + " to the group",
                adder, group, newMember.getId(), "USER"
        );

        return entityMapper.toGroupResponse(group);
    }

    private User createGuestUser(String name) {
        String uniqueEmail = "guest_" + UUID.randomUUID() + "@guest.splitwise.local";
        User guest = User.builder()
                .name(name)
                .email(uniqueEmail)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .registered(false)
                .build();
        return userRepository.save(guest);
    }

    private void validateMembership(Group group, Long userId) {
        boolean isMember = group.getMembers().stream()
                .anyMatch(m -> m.getId().equals(userId));
        if (!isMember) {
            throw new BadRequestException("You are not a member of this group");
        }
    }
}
