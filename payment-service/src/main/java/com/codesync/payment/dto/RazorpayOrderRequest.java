package com.codesync.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayOrderRequest {
    private Integer amount; // amount in paise
    private String currency;
}
