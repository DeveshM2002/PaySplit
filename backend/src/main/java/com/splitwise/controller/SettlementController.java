package com.splitwise.controller;

import com.splitwise.dto.request.CreateSettlementRequest;
import com.splitwise.dto.response.SettlementResponse;
import com.splitwise.security.CurrentUser;
import com.splitwise.security.UserPrincipal;
import com.splitwise.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Tag(name = "Settlements", description = "Debt settlement management")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @Operation(summary = "Record a settlement payment")
    public ResponseEntity<SettlementResponse> createSettlement(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateSettlementRequest request) {
        Long paidById = request.getPaidByUserId() != null ? request.getPaidByUserId() : currentUser.getId();
        SettlementResponse settlement = settlementService.createSettlement(paidById, request);
        return new ResponseEntity<>(settlement, HttpStatus.CREATED);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get all settlements in a group")
    public ResponseEntity<List<SettlementResponse>> getGroupSettlements(@PathVariable Long groupId) {
        return ResponseEntity.ok(settlementService.getGroupSettlements(groupId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's settlements")
    public ResponseEntity<List<SettlementResponse>> getMySettlements(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(settlementService.getUserSettlements(currentUser.getId()));
    }

    @GetMapping("/with/{userId}")
    @Operation(summary = "Get settlements between you and another user")
    public ResponseEntity<List<SettlementResponse>> getSettlementsWithUser(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long userId) {
        return ResponseEntity.ok(settlementService.getSettlementsBetweenUsers(
                currentUser.getId(), userId));
    }
}
