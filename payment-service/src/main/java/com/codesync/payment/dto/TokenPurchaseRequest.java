package com.codesync.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenPurchaseRequest {

    @NotNull private Integer userId;
    @NotNull private Integer projectId;
    @NotNull private Integer fileId;
    @NotNull @Min(1) private Integer tokensToBuy;

    /** Optional: gateway payment reference to verify before crediting tokens. */
    private String gatewayReference;

    /**
     * Developer's email address — used by the Notification Service to dispatch
     * payment confirmation / failure emails via Kafka.
     * Optional: if absent the notification service will skip email delivery.
     */
    private String email;

    /**
     * Developer's display name — used in the notification email body.
     * Falls back to "User-{userId}" if not provided.
     */
    private String userName;
}
