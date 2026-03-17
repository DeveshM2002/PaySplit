package com.splitwise.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberByNameRequest {

    @NotBlank(message = "Member name is required")
    private String name;
}
