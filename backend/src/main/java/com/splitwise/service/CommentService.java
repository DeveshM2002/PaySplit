package com.splitwise.service;

import com.splitwise.dto.request.AddCommentRequest;
import com.splitwise.dto.response.CommentResponse;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.mapper.EntityMapper;
import com.splitwise.model.Comment;
import com.splitwise.model.Expense;
import com.splitwise.model.User;
import com.splitwise.model.enums.ActivityType;
import com.splitwise.repository.CommentRepository;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final EntityMapper entityMapper;

    @Transactional
    public CommentResponse addComment(Long expenseId, Long userId, AddCommentRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .expense(expense)
                .author(user)
                .build();

        comment = commentRepository.save(comment);

        activityService.logActivity(
                ActivityType.COMMENT_ADDED,
                user.getName() + " commented on \"" + expense.getDescription() + "\"",
                user, expense.getGroup(), expense.getId(), "EXPENSE"
        );

        return entityMapper.toCommentResponse(comment);
    }

    public List<CommentResponse> getExpenseComments(Long expenseId) {
        return commentRepository.findByExpenseIdOrderByCreatedAtAsc(expenseId).stream()
                .map(entityMapper::toCommentResponse)
                .collect(Collectors.toList());
    }
}
