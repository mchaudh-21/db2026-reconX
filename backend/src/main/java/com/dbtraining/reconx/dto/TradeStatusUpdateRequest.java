package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * TICKET-ADV066 — narrow request contract for the status sub-resource.
 *
 * Keeping PATCH separate from TradeRequest prevents an accidental partial
 * update of quantity, price, side, instrument, or counterparty.
 */
public record TradeStatusUpdateRequest(
        @NotBlank(message = "status is required")
        @Pattern(
                regexp = "^(PENDING|MATCHED|BREAK|CANCELLED)$",
                message = "status must be one of PENDING, MATCHED, BREAK, CANCELLED"
        )
        String status
) {
}
