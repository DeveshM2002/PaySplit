package com.splitwise.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySuggestionResponse {
    private String topCategory;
    private List<String> alternates;
    private String rationale;
}
