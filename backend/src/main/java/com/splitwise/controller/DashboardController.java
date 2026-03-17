package com.splitwise.controller;

import com.splitwise.dto.response.ActivityResponse;
import com.splitwise.dto.response.BalanceResponse;
import com.splitwise.dto.response.DashboardResponse;
import com.splitwise.security.CurrentUser;
import com.splitwise.security.UserPrincipal;
import com.splitwise.service.ActivityService;
import com.splitwise.service.BalanceService;
import com.splitwise.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard and analytics")
public class DashboardController {

    private final DashboardService dashboardService;
    private final BalanceService balanceService;
    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "Get dashboard data (balances, activity, analytics)")
    public ResponseEntity<DashboardResponse> getDashboard(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(dashboardService.getDashboard(currentUser.getId()));
    }

    @GetMapping("/balances")
    @Operation(summary = "Get overall balance summary")
    public ResponseEntity<BalanceResponse> getOverallBalances(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(balanceService.getUserOverallBalance(currentUser.getId()));
    }

    @GetMapping("/activity")
    @Operation(summary = "Get activity feed (paginated)")
    public ResponseEntity<Page<ActivityResponse>> getActivityFeed(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(activityService.getUserActivities(currentUser.getId(), page, size));
    }

    @GetMapping("/groups/{groupId}/activity")
    @Operation(summary = "Get group activity feed")
    public ResponseEntity<List<ActivityResponse>> getGroupActivity(@PathVariable Long groupId) {
        return ResponseEntity.ok(activityService.getGroupActivities(groupId));
    }
}
