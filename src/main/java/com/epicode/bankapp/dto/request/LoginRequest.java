package com.epicode.bankapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "l'email è obbligatoria")
    @Email(message = "email non valida")
    private String email;

    @NotBlank(message = "la password è obbligatoria")
    private String password;
}
