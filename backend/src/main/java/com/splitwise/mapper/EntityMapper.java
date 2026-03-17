package com.splitwise.mapper;

import com.splitwise.dto.response.*;
import com.splitwise.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts entities to response DTOs.
 *
 * WHY a manual mapper instead of MapStruct?
 * - MapStruct generates mapping code at compile time (faster than reflection-based tools)
 * - But it requires annotation processing setup in Maven and can be confusing to debug
 * - For a learning project, explicit mapping code is easier to understand
 * - You can SEE exactly what's happening — no "magic"
 *
 * Alternative: ModelMapper (reflection-based, slower, auto-maps matching field names)
 * Alternative: MapStruct (compile-time code generation, fastest, but adds build complexity)
 * We chose manual mapping for transparency. In a production project, use MapStruct.
 */
@Component
public class EntityMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .registered(user.getRegistered())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public GroupResponse toGroupResponse(Group group) {
        if (group == null) return null;
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .imageUrl(group.getImageUrl())
                .createdBy(toUserResponse(group.getCreatedBy()))
                .members(group.getMembers().stream()
                        .map(this::toUserResponse)
                        .collect(Collectors.toList()))
                .simplifyDebts(group.getSimplifyDebts())
                .createdAt(group.getCreatedAt())
                .build();
    }

    public ExpenseResponse toExpenseResponse(Expense expense) {
        if (expense == null) return null;
        return ExpenseResponse.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .splitType(expense.getSplitType())
                .category(expense.getCategory())
                .date(expense.getDate())
                .paidBy(toUserResponse(expense.getPaidBy()))
                .groupId(expense.getGroup() != null ? expense.getGroup().getId() : null)
                .groupName(expense.getGroup() != null ? expense.getGroup().getName() : null)
                .splits(expense.getSplits().stream()
                        .map(this::toExpenseSplitResponse)
                        .collect(Collectors.toList()))
                .isRecurring(expense.getIsRecurring())
                .currency(expense.getCurrency())
                .commentCount(expense.getComments().size())
                .createdAt(expense.getCreatedAt())
                .build();
    }

    public ExpenseResponse.ExpenseSplitResponse toExpenseSplitResponse(ExpenseSplit split) {
        if (split == null) return null;
        return ExpenseResponse.ExpenseSplitResponse.builder()
                .id(split.getId())
                .user(toUserResponse(split.getUser()))
                .amount(split.getAmount())
                .percentage(split.getPercentage())
                .build();
    }

    public SettlementResponse toSettlementResponse(Settlement settlement) {
        if (settlement == null) return null;
        return SettlementResponse.builder()
                .id(settlement.getId())
                .paidBy(toUserResponse(settlement.getPaidBy()))
                .paidTo(toUserResponse(settlement.getPaidTo()))
                .amount(settlement.getAmount())
                .groupId(settlement.getGroup() != null ? settlement.getGroup().getId() : null)
                .date(settlement.getDate())
                .notes(settlement.getNotes())
                .createdAt(settlement.getCreatedAt())
                .build();
    }

    public ActivityResponse toActivityResponse(Activity activity) {
        if (activity == null) return null;
        return ActivityResponse.builder()
                .id(activity.getId())
                .type(activity.getType())
                .description(activity.getDescription())
                .performedBy(toUserResponse(activity.getPerformedBy()))
                .groupId(activity.getGroup() != null ? activity.getGroup().getId() : null)
                .relatedEntityId(activity.getRelatedEntityId())
                .relatedEntityType(activity.getRelatedEntityType())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    public CommentResponse toCommentResponse(Comment comment) {
        if (comment == null) return null;
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(toUserResponse(comment.getAuthor()))
                .expenseId(comment.getExpense().getId())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public <T> List<T> mapList(List<?> source, java.util.function.Function<Object, T> mapper) {
        return source.stream().map(mapper).collect(Collectors.toList());
    }
}
