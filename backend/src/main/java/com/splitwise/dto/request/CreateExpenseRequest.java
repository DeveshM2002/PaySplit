package com.splitwise.dto.request;

import com.splitwise.model.enums.ExpenseCategory;
import com.splitwise.model.enums.SplitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * WHY DTOs instead of using the Entity directly?
 *
 * 1. SECURITY: Entities may have fields you don't want clients to set
 *    (e.g., User.password shouldn't be in the response, User.id shouldn't be in request)
 * 2. FLEXIBILITY: The API shape can differ from the DB schema
 *    (e.g., client sends "memberIds" as a list of Longs, but the Entity has a Set<User>)
 * 3. VALIDATION: DTOs have request-specific validation that doesn't belong on entities
 * 4. VERSIONING: You can change DTOs without affecting the database schema
 *
 * Alternative: Use entities directly with @JsonIgnore on sensitive fields.
 * WHY NOT: Tight coupling between API contract and DB schema. One change breaks both.
 */
@Data
public class CreateExpenseRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    private ExpenseCategory category = ExpenseCategory.OTHER;

    private LocalDate date;

    private Long groupId;

    @NotNull(message = "Splits are required")
    private List<SplitDetail> splits;

    private Long paidById;

    private Boolean isRecurring = false;
    private String recurringInterval;
    private String currency = "INR";

    @Data
    public static class SplitDetail {
        @NotNull
        private Long userId;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
