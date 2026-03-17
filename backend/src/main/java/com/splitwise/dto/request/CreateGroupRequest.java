package com.splitwise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private String imageUrl;

    private List<Long> memberIds;

    private List<String> memberNames;

    private Boolean simplifyDebts = true;
}
