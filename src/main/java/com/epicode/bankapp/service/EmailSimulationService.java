package com.epicode.bankapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Nessun invio email reale: la consegna richiede solo la "simulazione" del funzionamento
 * bancario, quindi il contenuto delle email viene semplicemente stampato in console.
 */
@Slf4j
@Service
public class EmailSimulationService {

    public void sendConfirmationEmail(String to, String confirmationLink) {
        log.info("""

                ================= EMAIL SIMULATA =================
                A: {}
                Oggetto: Conferma la tua registrazione
                Corpo: Benvenuto! Clicca sul link seguente per attivare il tuo account:
                {}
                ====================================================
                """, to, confirmationLink);
    }

    public void sendLoginOtpEmail(String to, String code) {
        log.info("""

                ================= EMAIL SIMULATA =================
                A: {}
                Oggetto: Codice di accesso
                Corpo: Il tuo codice di accesso e': {}  (valido pochi minuti)
                ====================================================
                """, to, code);
    }

    public void sendTransferOtpEmail(String to, String code, BigDecimal amount, String recipientEmail) {
        log.info("""

                ================= EMAIL SIMULATA =================
                A: {}
                Oggetto: Autorizza il tuo bonifico
                Corpo: Hai richiesto un bonifico di {} EUR verso {}.
                Codice di autorizzazione: {}  (valido pochi minuti)
                Se non hai richiesto tu questa operazione, ignora questa email.
                ====================================================
                """, to, amount, recipientEmail, code);
    }
}
