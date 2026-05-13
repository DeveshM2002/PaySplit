package com.splitwise.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.ai.client.LlmClient;
import com.splitwise.ai.dto.*;
import com.splitwise.ai.prompt.PromptTemplates;
import com.splitwise.dto.response.*;
import com.splitwise.exception.BadRequestException;
import com.splitwise.model.Expense;
import com.splitwise.model.ExpenseSplit;
import com.splitwise.model.enums.ExpenseCategory;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final LlmClient llmClient;
    private final DashboardService dashboardService;
    private final ActivityService activityService;
    private final BalanceService balanceService;
    private final ExpenseService expenseService;
    private final GroupService groupService;
    private final ExpenseRepository expenseRepository;
    private final ObjectMapper objectMapper;

    // ========== Explain Balance ==========

    public AssistantResponse explainBalance(Long userId, Long focusGroupId) {
        DashboardResponse dashboard = dashboardService.getDashboard(userId);
        Page<ActivityResponse> activity = activityService.getUserActivities(userId, 0, 10);

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("totalOwed", dashboard.getTotalOwed());
        facts.put("totalOwing", dashboard.getTotalOwing());
        facts.put("netBalance", dashboard.getNetBalance());
        facts.put("friendBalances", dashboard.getFriendBalances());
        facts.put("groupBalances", dashboard.getGroupBalances());
        facts.put("recentActivity", activity.getContent());
        facts.put("categorySpending", dashboard.getCategorySpending());

        if (focusGroupId != null) {
            groupService.getGroupById(focusGroupId, userId);
            BalanceResponse groupBalance = balanceService.getGroupBalance(focusGroupId, userId);
            facts.put("focusGroupBalance", groupBalance);
        }

        String factsJson = toJson(facts);
        String narrative = llmClient.complete(PromptTemplates.EXPLAIN_BALANCE, factsJson);

        List<AssistantResponse.Citation> citations = new ArrayList<>();
        if (dashboard.getFriendBalances() != null) {
            dashboard.getFriendBalances().forEach(fb ->
                citations.add(AssistantResponse.Citation.builder()
                    .type("friend_balance").id(fb.getFriendId()).label(fb.getFriendName()).build()));
        }
        if (dashboard.getGroupBalances() != null) {
            dashboard.getGroupBalances().forEach(gb ->
                citations.add(AssistantResponse.Citation.builder()
                    .type("group_balance").id(gb.getGroupId()).label(gb.getGroupName()).build()));
        }

        return AssistantResponse.builder()
            .narrative(narrative)
            .facts(facts)
            .citations(citations)
            .build();
    }

    // ========== Expense Search / Q&A ==========

    /**
     * Must run in a read transaction: expense rows are loaded from the repository and we touch
     * lazy associations (paidBy, group) while building the facts JSON.
     */
    @Transactional(readOnly = true)
    public AssistantResponse expenseQnA(Long userId, String question, String searchQuery, Long groupId, Object chartSummary) {
        if (question == null || question.isBlank()) {
            throw new BadRequestException("question is required");
        }
        if (groupId != null) {
            groupService.getGroupById(groupId, userId);
        }
        List<Expense> expenses;
        if (searchQuery != null && !searchQuery.isBlank()) {
            expenses = searchExpenses(userId, searchQuery, groupId);
        } else {
            expenses = expenseRepository.findExpensesInvolvingUser(userId);
            if (expenses.size() > 30) {
                expenses = expenses.subList(0, 30);
            }
        }

        List<Map<String, Object>> expenseHits = expenses.stream().map(e -> {
            Map<String, Object> hit = new LinkedHashMap<>();
            hit.put("id", e.getId());
            hit.put("description", e.getDescription());
            hit.put("amount", e.getAmount());
            hit.put("category", e.getCategory().name());
            hit.put("date", e.getDate().toString());
            hit.put("paidBy", e.getPaidBy() != null ? e.getPaidBy().getName() : "Unknown");
            if (e.getGroup() != null) {
                hit.put("groupId", e.getGroup().getId());
                hit.put("groupName", e.getGroup().getName());
            }
            return hit;
        }).collect(Collectors.toList());

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("matchingExpenses", expenseHits);
        facts.put("resultCount", expenseHits.size());
        if (chartSummary != null) {
            facts.put("chartSummary", chartSummary);
        }

        String userMsg = "Question: " + question + "\n\nData:\n" + toJson(facts);
        String narrative = llmClient.complete(PromptTemplates.EXPENSE_QNA, userMsg);

        List<AssistantResponse.Citation> citations = expenses.stream()
            .map(e -> AssistantResponse.Citation.builder()
                .type("expense").id(e.getId()).label(e.getDescription()).build())
            .collect(Collectors.toList());

        return AssistantResponse.builder()
            .narrative(narrative)
            .facts(facts)
            .citations(citations)
            .build();
    }

    private List<Expense> searchExpenses(Long userId, String query, Long groupId) {
        Page<Expense> page = expenseRepository.searchInvolvingUserByKeyword(
                userId, query, PageRequest.of(0, 40, Sort.by("date").descending()));
        return page.getContent().stream()
                .filter(e -> groupId == null || (e.getGroup() != null && e.getGroup().getId().equals(groupId)))
                .limit(30)
                .collect(Collectors.toList());
    }

    // ========== Guided Expense Entry ==========

    public ExpenseDraftResponse expenseDraft(Long userId, String utterance, Long defaultGroupId, String defaultCurrency) {
        if (utterance == null || utterance.isBlank()) {
            throw new BadRequestException("utterance is required");
        }
        List<GroupResponse> userGroups = groupService.getUserGroups(userId);

        List<Map<String, Object>> groupsContext = userGroups.stream().map(g -> {
            Map<String, Object> gm = new LinkedHashMap<>();
            gm.put("id", g.getId());
            gm.put("name", g.getName());
            if (g.getMembers() != null) {
                gm.put("members", g.getMembers().stream()
                    .map(m -> Map.of("id", m.getId(), "name", m.getName()))
                    .collect(Collectors.toList()));
            }
            return gm;
        }).collect(Collectors.toList());

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("currentUserId", userId);
        context.put("defaultCurrency", defaultCurrency != null ? defaultCurrency : "INR");
        if (defaultGroupId != null) context.put("defaultGroupId", defaultGroupId);
        context.put("availableGroups", groupsContext);

        String userMsg = "Utterance: \"" + utterance + "\"\n\nContext:\n" + toJson(context);
        String jsonResponse = llmClient.completeWithJsonMode(PromptTemplates.EXPENSE_DRAFT, userMsg);

        try {
            JsonNode parsed = objectMapper.readTree(jsonResponse);

            ExpenseDraftResponse.ExpenseDraft draft = ExpenseDraftResponse.ExpenseDraft.builder()
                .description(parsed.path("description").asText(""))
                .amount(new BigDecimal(parsed.path("amount").asText("0")))
                .splitType(com.splitwise.model.enums.SplitType.EQUAL)
                .category(parseCategory(parsed.path("category").asText("OTHER")))
                .date(LocalDate.now().toString())
                .groupId(parsed.path("groupId").isNull() ? null : parsed.path("groupId").asLong())
                .groupName(parsed.path("groupName").asText(null))
                .paidById(parsed.path("paidById").isNull() ? userId : parsed.path("paidById").asLong())
                .paidByName(parsed.path("paidByName").asText(null))
                .currency(parsed.path("currency").asText(defaultCurrency != null ? defaultCurrency : "INR"))
                .build();

            JsonNode memberIds = parsed.path("memberIds");
            if (memberIds.isArray() && !memberIds.isEmpty()) {
                BigDecimal equalShare = draft.getAmount()
                    .divide(BigDecimal.valueOf(memberIds.size()), 2, RoundingMode.HALF_UP);
                List<ExpenseDraftResponse.SplitDetail> splits = new ArrayList<>();
                for (JsonNode mid : memberIds) {
                    splits.add(ExpenseDraftResponse.SplitDetail.builder()
                        .userId(mid.asLong())
                        .amount(equalShare)
                        .build());
                }
                draft.setSplits(splits);
            }

            String confidence = parsed.path("confidence").asText("MEDIUM");
            List<String> ambiguities = new ArrayList<>();
            if (parsed.has("ambiguities") && parsed.path("ambiguities").isArray()) {
                for (JsonNode a : parsed.path("ambiguities")) {
                    ambiguities.add(a.asText());
                }
            }

            if (draft.getGroupId() != null
                    && draft.getAmount() != null
                    && draft.getAmount().compareTo(BigDecimal.ZERO) > 0
                    && (draft.getSplits() == null || draft.getSplits().isEmpty())) {
                enrichEqualSplitsAmongGroupMembers(draft, userId);
            }

            return ExpenseDraftResponse.builder()
                .narrative("Parsed your expense description. Please review and confirm.")
                .draft(draft)
                .needsConfirmation(!"HIGH".equals(confidence))
                .confirmationMessage(ambiguities.isEmpty() ? null : "Uncertainties: " + String.join(", ", ambiguities))
                .build();

        } catch (Exception e) {
            log.error("Failed to parse expense draft response", e);
            return ExpenseDraftResponse.builder()
                .narrative("I couldn't fully understand that. Please try rephrasing or fill in the form manually.")
                .needsConfirmation(true)
                .build();
        }
    }

    // ========== Intelligent Categorization ==========

    public CategorySuggestionResponse suggestCategory(String description, String merchant) {
        if (description == null || description.isBlank()) {
            throw new BadRequestException("description is required");
        }
        String input = "Description: " + description;
        if (merchant != null && !merchant.isBlank()) {
            input += "\nMerchant: " + merchant;
        }

        String jsonResponse = llmClient.completeWithJsonMode(PromptTemplates.SUGGEST_CATEGORY, input);

        try {
            JsonNode parsed = objectMapper.readTree(jsonResponse);
            List<String> alternates = new ArrayList<>();
            if (parsed.has("alternates") && parsed.path("alternates").isArray()) {
                for (JsonNode alt : parsed.path("alternates")) {
                    alternates.add(alt.asText());
                }
            }
            return CategorySuggestionResponse.builder()
                .topCategory(parsed.path("topCategory").asText("OTHER"))
                .alternates(alternates)
                .rationale(parsed.path("rationale").asText(""))
                .build();
        } catch (Exception e) {
            log.error("Failed to parse category suggestion", e);
            return CategorySuggestionResponse.builder()
                .topCategory("OTHER")
                .alternates(List.of())
                .rationale("Could not determine category")
                .build();
        }
    }

    // ========== Smart Settlement Suggestions ==========

    public AssistantResponse settlementBriefing(Long groupId, Long userId) {
        GroupResponse group = groupService.getGroupById(groupId, userId);
        List<BalanceResponse.DebtEntry> debts = balanceService.getGroupDebts(groupId);
        List<BalanceResponse.DebtEntry> simplified = balanceService.getSimplifiedDebts(groupId);
        List<BalanceResponse.UserBalance> memberBalances = balanceService.getGroupMemberBalances(groupId);

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("groupName", group.getName());
        facts.put("memberCount", group.getMembers() != null ? group.getMembers().size() : 0);
        facts.put("pairwiseDebts", debts);
        facts.put("simplifiedDebts", simplified);
        facts.put("memberBalances", memberBalances);

        String narrative = llmClient.complete(PromptTemplates.SETTLEMENT_BRIEFING, toJson(facts));

        List<AssistantResponse.Citation> citations = new ArrayList<>();
        simplified.forEach(d ->
            citations.add(AssistantResponse.Citation.builder()
                .type("settlement_suggestion")
                .label(d.getFrom().getName() + " -> " + d.getTo().getName() + ": " + d.getAmount())
                .build()));

        return AssistantResponse.builder()
            .narrative(narrative)
            .facts(facts)
            .citations(citations)
            .build();
    }

    // ========== Activity-Feed Narrator ==========

    public AssistantResponse activityNarrative(Long userId, String scope, Long groupId, int pageSize) {
        List<ActivityResponse> activities;

        if ("GROUP".equalsIgnoreCase(scope) && groupId != null) {
            groupService.getGroupById(groupId, userId);
            activities = activityService.getGroupActivities(groupId);
            if (activities.size() > pageSize) {
                activities = activities.subList(0, pageSize);
            }
        } else {
            Page<ActivityResponse> page = activityService.getUserActivities(userId, 0, pageSize);
            activities = page.getContent();
        }

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("scope", scope);
        facts.put("activityCount", activities.size());
        facts.put("activities", activities);

        String narrative = llmClient.complete(PromptTemplates.ACTIVITY_NARRATIVE, toJson(facts));

        List<AssistantResponse.Citation> citations = activities.stream()
            .map(a -> AssistantResponse.Citation.builder()
                .type("activity").id(a.getId()).label(a.getDescription()).build())
            .collect(Collectors.toList());

        return AssistantResponse.builder()
            .narrative(narrative)
            .facts(facts)
            .citations(citations)
            .build();
    }

    // ========== What-If Explorer ==========

    @Transactional(readOnly = true)
    public WhatIfResponse whatIf(Long userId, List<String> excludeCategories, List<Long> excludeExpenseIds, Long groupId) {
        if (groupId != null) {
            groupService.getGroupById(groupId, userId);
        }
        List<Expense> allExpenses = expenseRepository.findExpensesInvolvingUser(userId);

        Set<String> excludeCats = excludeCategories != null
            ? excludeCategories.stream().map(String::toUpperCase).collect(Collectors.toSet())
            : Collections.emptySet();
        Set<Long> excludeIds = excludeExpenseIds != null
            ? new HashSet<>(excludeExpenseIds)
            : Collections.emptySet();

        Map<String, BigDecimal> originalCategoryTotals = new TreeMap<>();
        Map<String, BigDecimal> adjustedCategoryTotals = new TreeMap<>();
        Map<String, BigDecimal> originalMonthlyTotals = new TreeMap<>();
        Map<String, BigDecimal> adjustedMonthlyTotals = new TreeMap<>();
        BigDecimal originalTotal = BigDecimal.ZERO;
        BigDecimal adjustedTotal = BigDecimal.ZERO;

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

        for (Expense expense : allExpenses) {
            if (groupId != null && (expense.getGroup() == null || !expense.getGroup().getId().equals(groupId))) {
                continue;
            }

            BigDecimal userShare = BigDecimal.ZERO;
            for (ExpenseSplit split : expense.getSplits()) {
                if (split.getUser().getId().equals(userId)) {
                    userShare = split.getAmount();
                    break;
                }
            }

            String catKey = expense.getCategory().name();
            originalCategoryTotals.merge(catKey, userShare, BigDecimal::add);
            originalTotal = originalTotal.add(userShare);

            if (expense.getDate().isAfter(sixMonthsAgo)) {
                String monthKey = expense.getDate().getYear() + "-"
                    + String.format("%02d", expense.getDate().getMonthValue());
                originalMonthlyTotals.merge(monthKey, userShare, BigDecimal::add);
            }

            boolean excluded = excludeCats.contains(catKey) || excludeIds.contains(expense.getId());
            if (!excluded) {
                adjustedCategoryTotals.merge(catKey, userShare, BigDecimal::add);
                adjustedTotal = adjustedTotal.add(userShare);
                if (expense.getDate().isAfter(sixMonthsAgo)) {
                    String monthKey = expense.getDate().getYear() + "-"
                        + String.format("%02d", expense.getDate().getMonthValue());
                    adjustedMonthlyTotals.merge(monthKey, userShare, BigDecimal::add);
                }
            }
        }

        WhatIfResponse result = WhatIfResponse.builder()
            .originalCategoryTotals(originalCategoryTotals)
            .adjustedCategoryTotals(adjustedCategoryTotals)
            .originalMonthlyTotals(originalMonthlyTotals)
            .adjustedMonthlyTotals(adjustedMonthlyTotals)
            .originalTotal(originalTotal)
            .adjustedTotal(adjustedTotal)
            .difference(originalTotal.subtract(adjustedTotal))
            .build();

        try {
            String narrative = llmClient.complete(PromptTemplates.WHAT_IF, toJson(result));
            result.setNarrative(narrative);
        } catch (Exception e) {
            log.warn("LLM narration failed for what-if, returning data only", e);
            result.setNarrative("Excluded items would save you " + originalTotal.subtract(adjustedTotal) + " in total spending.");
        }

        return result;
    }

    // ========== Chart Captioning ==========

    public AssistantResponse chartCaption(String chartKind, Object series) {
        Map<String, Object> facts = Map.of(
            "chartKind", chartKind,
            "series", series
        );

        String narrative = llmClient.complete(PromptTemplates.CHART_CAPTION, toJson(facts));

        return AssistantResponse.builder()
            .narrative(narrative)
            .facts(facts)
            .citations(List.of())
            .build();
    }

    // ========== Spending Coherence Check ==========

    public List<CoherenceFlag> coherenceScan(Long userId, Long groupId) {
        if (groupId != null) {
            groupService.getGroupById(groupId, userId);
        }
        List<Expense> expenses;
        if (groupId != null) {
            expenses = expenseRepository.findByGroupIdOrderByDateDesc(groupId);
        } else {
            expenses = expenseRepository.findExpensesInvolvingUser(userId);
        }

        List<CoherenceFlag> flags = new ArrayList<>();

        for (int i = 0; i < expenses.size(); i++) {
            for (int j = i + 1; j < expenses.size() && j < i + 20; j++) {
                Expense e1 = expenses.get(i);
                Expense e2 = expenses.get(j);

                if (e1.getDate().equals(e2.getDate())
                    && e1.getAmount().compareTo(e2.getAmount()) == 0
                    && descriptionSimilarity(e1.getDescription(), e2.getDescription()) > 0.7) {
                    flags.add(CoherenceFlag.builder()
                        .flagType("POSSIBLE_DUPLICATE")
                        .expenseId1(e1.getId())
                        .expense1Description(e1.getDescription())
                        .expenseId2(e2.getId())
                        .expense2Description(e2.getDescription())
                        .reason("Same date, same amount, similar description")
                        .build());
                }
            }

            Expense e = expenses.get(i);
            String categoryMismatch = checkCategoryMismatch(e.getDescription(), e.getCategory());
            if (categoryMismatch != null) {
                flags.add(CoherenceFlag.builder()
                    .flagType("CATEGORY_MISMATCH")
                    .expenseId1(e.getId())
                    .expense1Description(e.getDescription())
                    .reason(categoryMismatch)
                    .build());
            }
        }

        return flags;
    }

    // ========== Per-Group Digest ==========

    public AssistantResponse groupDigest(Long groupId, Long userId) {
        GroupResponse group = groupService.getGroupById(groupId, userId);
        List<ExpenseResponse> expenses = expenseService.getGroupExpenses(groupId);
        List<ActivityResponse> activities = activityService.getGroupActivities(groupId);
        List<BalanceResponse.DebtEntry> simplifiedDebts = balanceService.getSimplifiedDebts(groupId);
        List<BalanceResponse.UserBalance> memberBalances = balanceService.getGroupMemberBalances(groupId);

        if (expenses.size() > 50) expenses = expenses.subList(0, 50);
        if (activities.size() > 30) activities = activities.subList(0, 30);

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("group", Map.of(
            "name", group.getName(),
            "memberCount", group.getMembers() != null ? group.getMembers().size() : 0,
            "members", group.getMembers() != null ? group.getMembers().stream()
                .map(m -> Map.of("id", m.getId(), "name", m.getName())).collect(Collectors.toList()) : List.of()
        ));
        facts.put("recentExpenses", expenses.stream().map(e -> Map.of(
            "id", e.getId(),
            "description", e.getDescription(),
            "amount", e.getAmount(),
            "category", e.getCategory().name(),
            "date", e.getDate().toString(),
            "paidBy", e.getPaidBy().getName()
        )).collect(Collectors.toList()));
        facts.put("simplifiedDebts", simplifiedDebts);
        facts.put("memberBalances", memberBalances);
        facts.put("recentActivity", activities);

        String narrative = llmClient.complete(PromptTemplates.GROUP_DIGEST, toJson(facts));

        List<AssistantResponse.Citation> citations = new ArrayList<>();
        expenses.stream().limit(5).forEach(e ->
            citations.add(AssistantResponse.Citation.builder()
                .type("expense").id(e.getId()).label(e.getDescription()).build()));

        return AssistantResponse.builder()
            .narrative(narrative)
            .facts(facts)
            .citations(citations)
            .build();
    }

    // ========== Helpers ==========

    private void enrichEqualSplitsAmongGroupMembers(ExpenseDraftResponse.ExpenseDraft draft, Long userId) {
        try {
            GroupResponse g = groupService.getGroupById(draft.getGroupId(), userId);
            if (g.getMembers() == null || g.getMembers().isEmpty()) {
                return;
            }
            int n = g.getMembers().size();
            BigDecimal share = draft.getAmount()
                    .divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            BigDecimal distributed = share.multiply(BigDecimal.valueOf(n));
            BigDecimal remainder = draft.getAmount().subtract(distributed);
            List<ExpenseDraftResponse.SplitDetail> splits = new ArrayList<>();
            for (int i = 0; i < g.getMembers().size(); i++) {
                UserResponse m = g.getMembers().get(i);
                BigDecimal amt = share;
                if (i == 0) {
                    amt = amt.add(remainder);
                }
                splits.add(ExpenseDraftResponse.SplitDetail.builder()
                        .userId(m.getId())
                        .userName(m.getName())
                        .amount(amt)
                        .build());
            }
            draft.setSplits(splits);
        } catch (Exception ex) {
            log.warn("Could not enrich equal splits for expense draft", ex);
        }
    }

    private double descriptionSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        String a = s1.toLowerCase().trim();
        String b = s2.toLowerCase().trim();
        if (a.equals(b)) return 1.0;

        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        int distance = levenshteinDistance(a, b);
        return 1.0 - ((double) distance / maxLen);
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private static final Map<String, ExpenseCategory> KEYWORD_CATEGORY_MAP = Map.ofEntries(
        Map.entry("uber", ExpenseCategory.TRANSPORT),
        Map.entry("ola", ExpenseCategory.TRANSPORT),
        Map.entry("cab", ExpenseCategory.TRANSPORT),
        Map.entry("taxi", ExpenseCategory.TRANSPORT),
        Map.entry("bus", ExpenseCategory.TRANSPORT),
        Map.entry("train", ExpenseCategory.TRANSPORT),
        Map.entry("flight", ExpenseCategory.TRAVEL),
        Map.entry("hotel", ExpenseCategory.TRAVEL),
        Map.entry("airbnb", ExpenseCategory.TRAVEL),
        Map.entry("swiggy", ExpenseCategory.FOOD),
        Map.entry("zomato", ExpenseCategory.FOOD),
        Map.entry("restaurant", ExpenseCategory.FOOD),
        Map.entry("lunch", ExpenseCategory.FOOD),
        Map.entry("dinner", ExpenseCategory.FOOD),
        Map.entry("breakfast", ExpenseCategory.FOOD),
        Map.entry("coffee", ExpenseCategory.FOOD),
        Map.entry("grocery", ExpenseCategory.GROCERIES),
        Map.entry("groceries", ExpenseCategory.GROCERIES),
        Map.entry("supermarket", ExpenseCategory.GROCERIES),
        Map.entry("bigbasket", ExpenseCategory.GROCERIES),
        Map.entry("rent", ExpenseCategory.RENT),
        Map.entry("electricity", ExpenseCategory.UTILITIES),
        Map.entry("water bill", ExpenseCategory.UTILITIES),
        Map.entry("wifi", ExpenseCategory.UTILITIES),
        Map.entry("internet", ExpenseCategory.UTILITIES),
        Map.entry("netflix", ExpenseCategory.SUBSCRIPTIONS),
        Map.entry("spotify", ExpenseCategory.SUBSCRIPTIONS),
        Map.entry("amazon prime", ExpenseCategory.SUBSCRIPTIONS),
        Map.entry("movie", ExpenseCategory.ENTERTAINMENT),
        Map.entry("cinema", ExpenseCategory.ENTERTAINMENT),
        Map.entry("shopping", ExpenseCategory.SHOPPING),
        Map.entry("amazon", ExpenseCategory.SHOPPING),
        Map.entry("flipkart", ExpenseCategory.SHOPPING),
        Map.entry("medicine", ExpenseCategory.HEALTHCARE),
        Map.entry("doctor", ExpenseCategory.HEALTHCARE),
        Map.entry("hospital", ExpenseCategory.HEALTHCARE),
        Map.entry("pharmacy", ExpenseCategory.HEALTHCARE),
        Map.entry("tuition", ExpenseCategory.EDUCATION),
        Map.entry("course", ExpenseCategory.EDUCATION),
        Map.entry("book", ExpenseCategory.EDUCATION)
    );

    private String checkCategoryMismatch(String description, ExpenseCategory currentCategory) {
        if (description == null) return null;
        String lower = description.toLowerCase();

        for (Map.Entry<String, ExpenseCategory> entry : KEYWORD_CATEGORY_MAP.entrySet()) {
            if (lower.contains(entry.getKey()) && entry.getValue() != currentCategory) {
                return "Description mentions '" + entry.getKey() + "' which typically maps to "
                    + entry.getValue().name() + ", but category is " + currentCategory.name();
            }
        }
        return null;
    }

    private ExpenseCategory parseCategory(String cat) {
        try {
            return ExpenseCategory.valueOf(cat.toUpperCase());
        } catch (Exception e) {
            return ExpenseCategory.OTHER;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
