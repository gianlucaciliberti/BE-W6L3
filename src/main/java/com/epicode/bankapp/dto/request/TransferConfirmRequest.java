package com.epicode.bankapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferConfirmRequest {

    @NotBlank(message = "il codice è obbligatorio")
    private String code;
}
