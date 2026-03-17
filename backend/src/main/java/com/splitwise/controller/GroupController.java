package com.splitwise.controller;

import com.splitwise.dto.request.AddMemberByNameRequest;
import com.splitwise.dto.request.CreateGroupRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.dto.response.BalanceResponse;
import com.splitwise.dto.response.GroupResponse;
import com.splitwise.security.CurrentUser;
import com.splitwise.security.UserPrincipal;
import com.splitwise.service.BalanceService;
import com.splitwise.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group management")
public class GroupController {

    private final GroupService groupService;
    private final BalanceService balanceService;

    @PostMapping
    @Operation(summary = "Create a new group")
    public ResponseEntity<GroupResponse> createGroup(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateGroupRequest request) {
        GroupResponse group = groupService.createGroup(currentUser.getId(), request);
        return new ResponseEntity<>(group, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all groups for current user")
    public ResponseEntity<List<GroupResponse>> getUserGroups(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(groupService.getUserGroups(currentUser.getId()));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group details")
    public ResponseEntity<GroupResponse> getGroup(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId, currentUser.getId()));
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Update group")
    public ResponseEntity<GroupResponse> updateGroup(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, currentUser.getId(), request));
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete group (creator only)")
    public ResponseEntity<ApiResponse> deleteGroup(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.deleteGroup(groupId, currentUser.getId()));
    }

    @PostMapping("/{groupId}/members/by-name")
    @Operation(summary = "Add member to group by name (creates guest user if not registered)")
    public ResponseEntity<GroupResponse> addMemberByName(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long groupId,
            @Valid @RequestBody AddMemberByNameRequest request) {
        return ResponseEntity.ok(groupService.addMemberByName(groupId, currentUser.getId(), request.getName()));
    }

    @PostMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Add member to group")
    public ResponseEntity<GroupResponse> addMember(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(groupService.addMember(groupId, currentUser.getId(), userId));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Remove member from group")
    public ResponseEntity<GroupResponse> removeMember(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(groupService.removeMember(groupId, currentUser.getId(), userId));
    }

    @GetMapping("/{groupId}/balances")
    @Operation(summary = "Get all pairwise debts within a group")
    public ResponseEntity<List<BalanceResponse.DebtEntry>> getGroupBalances(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(balanceService.getGroupDebts(groupId));
    }

    @GetMapping("/{groupId}/simplified-debts")
    @Operation(summary = "Get simplified debts for the group")
    public ResponseEntity<List<BalanceResponse.DebtEntry>> getSimplifiedDebts(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(balanceService.getSimplifiedDebts(groupId));
    }

    @GetMapping("/{groupId}/member-balances")
    @Operation(summary = "Get per-person net balance summary in a group")
    public ResponseEntity<List<BalanceResponse.UserBalance>> getMemberBalances(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(balanceService.getGroupMemberBalances(groupId));
    }
}
