package com.codesync.payment.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuotaResponseDto {
    private Integer userId;
    private Integer projectId;
    private Integer fileId;
    private int executionsUsed;
    private int freeLimit;
    private int purchasedTokens;
    private int totalAllowed;
    private int remaining;
    private boolean canExecute;
}
