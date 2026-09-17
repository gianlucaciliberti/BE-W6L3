package com.epicode.bankapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequest {

    @NotBlank(message = "l'email è obbligatoria")
    @Email(message = "email non valida")
    private String email;

    @NotBlank(message = "il codice è obbligatorio")
    private String code;
}
