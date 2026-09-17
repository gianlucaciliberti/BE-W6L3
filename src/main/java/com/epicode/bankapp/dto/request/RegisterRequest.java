package com.epicode.bankapp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterRequest {

    @NotBlank(message = "il nome è obbligatorio")
    private String firstName;

    @NotBlank(message = "il cognome è obbligatorio")
    private String lastName;

    @NotBlank(message = "l'email è obbligatoria")
    @Email(message = "email non valida")
    private String email;

    @NotBlank(message = "la password è obbligatoria")
    @Size(min = 6, message = "la password deve avere almeno 6 caratteri")
    private String password;

    @NotNull(message = "l'importo iniziale è obbligatorio")
    @DecimalMin(value = "0.0", message = "l'importo iniziale non può essere negativo")
    private BigDecimal initialBalance;
}
