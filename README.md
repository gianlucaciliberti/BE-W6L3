# BE-W6L3 — Simulazione app bancaria

Progetto Week 6 (Java & Servizi) — Epicode AI Full-Stack Developer, Consegna D1.

Simula il funzionamento minimo di un'app bancaria:

1. **Registrazione**: l'utente indica i propri dati e l'importo con cui parte il conto.
2. **Conferma via email**: l'account resta disabilitato finché non si apre il link di conferma ricevuto via email.
3. **Login**: con password, oppure richiedendo un codice OTP inviato via email. In entrambi i casi l'account deve essere già confermato.
4. **Bonifico**: il trasferimento di denaro resta "in sospeso" finché non viene autorizzato con un codice OTP inviato via email (verifica in due passaggi, come nelle banche reali).

Le email **non vengono inviate realmente**: il contenuto (link di conferma, codici OTP) viene stampato nei log della console dell'applicazione, per poter testare il flusso senza un account SMTP.

## Stack tecnico

- Java 21, Spring Boot 4.1.1 (Web, Data JPA, Security, Validation)
- PostgreSQL (driver `postgresql`)
- JWT (`jjwt` 0.12.6) per l'autenticazione stateless
- Lombok
- Maven (con Maven Wrapper incluso, non serve installare Maven)

## Setup

1. Serve un server PostgreSQL in locale (porta 5432). A differenza di MySQL, Postgres
   **non crea da solo** il database: va creato una volta sola con `psql` (o pgAdmin):

   ```powershell
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -c "CREATE DATABASE bankapp;"
   ```
   (ti verrà chiesta la password dell'utente `postgres` scelta durante l'installazione).

2. Apri `src/main/resources/application.properties` e imposta `spring.datasource.username` / `spring.datasource.password` con le tue credenziali reali (di default punta a `postgres`/`postgres`). Le tabelle vengono create/aggiornate automaticamente al primo avvio (`spring.jpa.hibernate.ddl-auto=update`).
3. Avvia l'applicazione:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

L'app parte sulla porta `8080`. Le "email" simulate compaiono nei log della console (cercale tra i marker `================= EMAIL SIMULATA =================`).

## Struttura del progetto

```
src/main/java/com/epicode/bankapp/
├── model/          entità JPA: User, ConfirmationToken, OtpCode, Transfer (+ enum di stato)
├── repository/      repository Spring Data JPA
├── dto/             request/response usati dai controller (mai le entità direttamente)
├── security/        JwtService, filtro JWT, UserDetailsService
├── config/          configurazione Spring Security (SecurityFilterChain)
├── service/         logica applicativa: AuthService, TransferService, EmailSimulationService
├── controller/       endpoint REST: AuthController, TransferController, AccountController
└── exception/        ApiException + gestore globale degli errori
```

## Flusso ed endpoint

### 1. Registrazione e conferma

```
POST /api/auth/register
{
  "firstName": "Mario",
  "lastName": "Rossi",
  "email": "mario.rossi@example.com",
  "password": "password123",
  "initialBalance": 1000
}
```
Crea l'utente con `enabled=false` e "invia" (logga) un'email con un link tipo
`http://localhost:8080/api/auth/confirm?token=<uuid>`.

```
GET /api/auth/confirm?token=<uuid>
```
Attiva l'account (`enabled=true`). Da qui in poi l'utente può accedere.

### 2. Login

Due strade equivalenti, entrambe richiedono l'account confermato:

```
POST /api/auth/login
{ "email": "mario.rossi@example.com", "password": "password123" }
```

oppure via OTP:

```
POST /api/auth/login/otp/request
{ "email": "mario.rossi@example.com" }
```
(logga un codice a 6 cifre valido 5 minuti), poi:

```
POST /api/auth/login/otp/verify
{ "email": "mario.rossi@example.com", "code": "123456" }
```

Entrambe le strade restituiscono lo stesso risultato: un JWT da usare come
`Authorization: Bearer <token>` nelle richieste successive.

### 3. Bonifico con autorizzazione in due passaggi

```
POST /api/transfers          (autenticato)
{ "recipientEmail": "altro.utente@example.com", "amount": 100 }
```
Crea il bonifico in stato `PENDING` (il denaro **non** si è ancora mosso) e logga un
codice OTP di autorizzazione inviato al mittente.

```
POST /api/transfers/{id}/confirm   (autenticato, stesso utente mittente)
{ "code": "654321" }
```
Solo con il codice corretto e non scaduto (5 minuti) l'importo viene scalato dal conto
del mittente e accreditato al destinatario; il bonifico passa a `CONFIRMED`. Codice
errato → `401`; codice scaduto → il bonifico passa a `EXPIRED`; saldo insufficiente al
momento della conferma → `CANCELLED`.

### 4. Controllo saldo

```
GET /api/account/me   (autenticato)
```
Restituisce email, nome completo e saldo corrente — utile per verificare che i bonifici
abbiano spostato davvero il denaro.

## Come testare

Il backend è puramente REST (nessuna pagina HTML), quindi va chiamato con un client
HTTP. Postman va benissimo, ma non è obbligatorio: bastano `curl`/`Invoke-RestMethod`,
o direttamente dentro VS Code con l'estensione **REST Client** (file `.http`) o
**Thunder Client** — utile se vuoi tenere backend e test nello stesso editor in cui
farai il frontend.

## Note

- `spring.jpa.hibernate.ddl-auto=update` va bene per lo sviluppo/la consegna; in un
  progetto reale si userebbero migrazioni versionate (es. Flyway).
- Il segreto JWT in `application.properties` è un valore di default per l'ambiente di
  sviluppo: in produzione andrebbe esternalizzato (variabile d'ambiente / vault).
