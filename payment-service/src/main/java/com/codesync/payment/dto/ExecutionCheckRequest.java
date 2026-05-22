package com.codesync.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for checking / consuming an execution slot.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExecutionCheckRequest {
    @NotNull private Integer userId;
    @NotNull private Integer projectId;
    @NotNull private Integer fileId;
}
