package com.epicode.bankapp.dto.response;

import java.math.BigDecimal;

public record AccountResponse(String email, String fullName, BigDecimal balance) {
}
