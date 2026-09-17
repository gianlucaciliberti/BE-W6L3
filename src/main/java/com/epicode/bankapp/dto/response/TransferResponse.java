package com.epicode.bankapp.dto.response;

import com.epicode.bankapp.model.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        String senderEmail,
        String recipientEmail,
        BigDecimal amount,
        TransferStatus status,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt
) {
}
