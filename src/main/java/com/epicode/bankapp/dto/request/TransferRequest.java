package com.epicode.bankapp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "l'email del destinatario è obbligatoria")
    @Email(message = "email destinatario non valida")
    private String recipientEmail;

    @NotNull(message = "l'importo è obbligatorio")
    @DecimalMin(value = "0.01", message = "l'importo deve essere maggiore di zero")
    private BigDecimal amount;
}
