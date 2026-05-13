package com.splitwise.ai;

import com.splitwise.ai.client.AiDisabledException;
import com.splitwise.ai.dto.*;
import com.splitwise.ai.service.AiAssistantService;
import com.splitwise.exception.BadRequestException;
import com.splitwise.security.CurrentUser;
import com.splitwise.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "AI-powered insights and suggestions")
public class AssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/explain-balance")
    @Operation(summary = "Explain user's balance in plain language")
    public ResponseEntity<AssistantResponse> explainBalance(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody(required = false) Map<String, Object> body) {
        Long focusGroupId = body != null && body.get("focusGroupId") != null
            ? ((Number) body.get("focusGroupId")).longValue() : null;
        return ResponseEntity.ok(aiAssistantService.explainBalance(currentUser.getId(), focusGroupId));
    }

    @PostMapping("/expense-qna")
    @Operation(summary = "Ask questions about your expenses")
    public ResponseEntity<AssistantResponse> expenseQnA(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        String searchQuery = (String) body.get("searchQuery");
        Long groupId = body.get("groupId") != null ? ((Number) body.get("groupId")).longValue() : null;
        Object chartSummary = body.get("chartSummary");
        return ResponseEntity.ok(aiAssistantService.expenseQnA(
            currentUser.getId(), question, searchQuery, groupId, chartSummary));
    }

    @PostMapping("/expense-draft")
    @Operation(summary = "Parse natural language into an expense draft")
    public ResponseEntity<ExpenseDraftResponse> expenseDraft(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody Map<String, Object> body) {
        String utterance = (String) body.get("utterance");
        Long defaultGroupId = body.get("defaultGroupId") != null
            ? ((Number) body.get("defaultGroupId")).longValue() : null;
        String defaultCurrency = (String) body.get("defaultCurrency");
        return ResponseEntity.ok(aiAssistantService.expenseDraft(
            currentUser.getId(), utterance, defaultGroupId, defaultCurrency));
    }

    @PostMapping("/suggest-category")
    @Operation(summary = "Suggest expense category from description")
    public ResponseEntity<CategorySuggestionResponse> suggestCategory(
            @RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            body = Map.of();
        }
        return ResponseEntity.ok(aiAssistantService.suggestCategory(
            body.get("description"), body.get("merchant")));
    }

    @PostMapping("/settlement-briefing")
    @Operation(summary = "Get AI-narrated settlement suggestions for a group")
    public ResponseEntity<AssistantResponse> settlementBriefing(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody Map<String, Object> body) {
        if (body == null || body.get("groupId") == null) {
            throw new BadRequestException("groupId is required");
        }
        Long groupId = ((Number) body.get("groupId")).longValue();
        return ResponseEntity.ok(aiAssistantService.settlementBriefing(groupId, currentUser.getId()));
    }

    @PostMapping("/activity-narrative")
    @Operation(summary = "Get a narrative summary of recent activity")
    public ResponseEntity<AssistantResponse> activityNarrative(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody Map<String, Object> body) {
        String scope = (String) body.getOrDefault("scope", "USER");
        Long groupId = body.get("groupId") != null ? ((Number) body.get("groupId")).longValue() : null;
        int pageSize = body.get("pageSize") != null ? ((Number) body.get("pageSize")).intValue() : 20;
        return ResponseEntity.ok(aiAssistantService.activityNarrative(
            currentUser.getId(), scope, groupId, pageSize));
    }

    @PostMapping("/what-if")
    @Operation(summary = "Explore hypothetical spending scenarios")
    public ResponseEntity<WhatIfResponse> whatIf(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> excludeCategories = (List<String>) body.get("excludeCategories");
        @SuppressWarnings("unchecked")
        List<Number> excludeExpenseIdsRaw = (List<Number>) body.get("excludeExpenseIds");
        List<Long> excludeExpenseIds = excludeExpenseIdsRaw != null
            ? excludeExpenseIdsRaw.stream().map(Number::longValue).toList() : null;
        Long groupId = body.get("groupId") != null ? ((Number) body.get("groupId")).longValue() : null;
        return ResponseEntity.ok(aiAssistantService.whatIf(
            currentUser.getId(), excludeCategories, excludeExpenseIds, groupId));
    }

    @PostMapping("/chart-caption")
    @Operation(summary = "Generate a caption for chart data")
    public ResponseEntity<AssistantResponse> chartCaption(
            @RequestBody Map<String, Object> body) {
        String chartKind = (String) body.getOrDefault("chartKind", "bar");
        Object series = body.get("series");
        return ResponseEntity.ok(aiAssistantService.chartCaption(chartKind, series));
    }

    @GetMapping("/coherence-scan")
    @Operation(summary = "Scan for potential duplicate or miscategorized expenses")
    public ResponseEntity<List<CoherenceFlag>> coherenceScan(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long groupId) {
        return ResponseEntity.ok(aiAssistantService.coherenceScan(currentUser.getId(), groupId));
    }

    @PostMapping("/group-digest")
    @Operation(summary = "Get a comprehensive digest for a group")
    public ResponseEntity<AssistantResponse> groupDigest(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody Map<String, Object> body) {
        Long groupId = ((Number) body.get("groupId")).longValue();
        return ResponseEntity.ok(aiAssistantService.groupDigest(groupId, currentUser.getId()));
    }

    @ExceptionHandler(AiDisabledException.class)
    public ResponseEntity<Map<String, String>> handleAiDisabled(AiDisabledException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", e.getMessage(), "code", "AI_DISABLED"));
    }
}
