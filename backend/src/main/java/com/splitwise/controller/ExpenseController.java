package com.splitwise.controller;

import com.splitwise.dto.request.AddCommentRequest;
import com.splitwise.dto.request.CreateExpenseRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.dto.response.CommentResponse;
import com.splitwise.dto.response.ExpenseResponse;
import com.splitwise.security.CurrentUser;
import com.splitwise.security.UserPrincipal;
import com.splitwise.service.CommentService;
import com.splitwise.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense management")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Create a new expense")
    public ResponseEntity<ExpenseResponse> createExpense(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateExpenseRequest request) {
        Long paidById = request.getPaidById() != null ? request.getPaidById() : currentUser.getId();
        ExpenseResponse expense = expenseService.createExpense(paidById, request);
        return new ResponseEntity<>(expense, HttpStatus.CREATED);
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get expense details")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseService.getExpenseById(expenseId));
    }

    @PutMapping("/{expenseId}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long expenseId,
            @Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(expenseId, currentUser.getId(), request));
    }

    @DeleteMapping("/{expenseId}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<ApiResponse> deleteExpense(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseService.deleteExpense(expenseId, currentUser.getId()));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get all expenses in a group")
    public ResponseEntity<List<ExpenseResponse>> getGroupExpenses(@PathVariable Long groupId) {
        return ResponseEntity.ok(expenseService.getGroupExpenses(groupId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's expenses (paginated)")
    public ResponseEntity<Page<ExpenseResponse>> getMyExpenses(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(expenseService.getUserExpenses(currentUser.getId(), page, size));
    }

    @GetMapping("/with/{userId}")
    @Operation(summary = "Get non-group expenses between you and another user")
    public ResponseEntity<List<ExpenseResponse>> getExpensesWithUser(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long userId) {
        return ResponseEntity.ok(expenseService.getExpensesBetweenUsers(currentUser.getId(), userId));
    }

    @PostMapping("/{expenseId}/comments")
    @Operation(summary = "Add a comment to an expense")
    public ResponseEntity<CommentResponse> addComment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long expenseId,
            @Valid @RequestBody AddCommentRequest request) {
        return new ResponseEntity<>(
                commentService.addComment(expenseId, currentUser.getId(), request),
                HttpStatus.CREATED);
    }

    @GetMapping("/{expenseId}/comments")
    @Operation(summary = "Get comments for an expense")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long expenseId) {
        return ResponseEntity.ok(commentService.getExpenseComments(expenseId));
    }
}
