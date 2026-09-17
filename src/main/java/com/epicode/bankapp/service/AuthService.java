package com.epicode.bankapp.service;

import com.epicode.bankapp.dto.request.LoginRequest;
import com.epicode.bankapp.dto.request.OtpRequestDto;
import com.epicode.bankapp.dto.request.OtpVerifyRequest;
import com.epicode.bankapp.dto.request.RegisterRequest;
import com.epicode.bankapp.dto.response.AuthResponse;
import com.epicode.bankapp.dto.response.MessageResponse;
import com.epicode.bankapp.model.ConfirmationToken;
import com.epicode.bankapp.model.OtpCode;
import com.epicode.bankapp.model.OtpPurpose;
import com.epicode.bankapp.model.User;
import com.epicode.bankapp.repository.ConfirmationTokenRepository;
import com.epicode.bankapp.repository.OtpCodeRepository;
import com.epicode.bankapp.repository.UserRepository;
import com.epicode.bankapp.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.epicode.bankapp.exception.ApiException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSimulationService emailSimulationService;

    @Value("${app.confirmation-token.expiration-hours}")
    private long confirmationTokenExpirationHours;

    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email già registrata", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .balance(request.getInitialBalance())
                .enabled(false)
                .build();
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = ConfirmationToken.builder()
                .token(token)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(confirmationTokenExpirationHours))
                .build();
        confirmationTokenRepository.save(confirmationToken);

        String confirmationLink = baseUrl + "/api/auth/confirm?token=" + token;
        emailSimulationService.sendConfirmationEmail(user.getEmail(), confirmationLink);

        return new MessageResponse("Registrazione avvenuta con successo. Controlla la tua email per confermare l'account.");
    }

    @Transactional
    public MessageResponse confirmAccount(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ApiException("Token di conferma non valido", HttpStatus.BAD_REQUEST));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new ApiException("Account già confermato", HttpStatus.BAD_REQUEST);
        }

        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Token di conferma scaduto", HttpStatus.BAD_REQUEST);
        }

        confirmationToken.setConfirmedAt(LocalDateTime.now());
        User user = confirmationToken.getUser();
        user.setEnabled(true);

        confirmationTokenRepository.save(confirmationToken);
        userRepository.save(user);

        return new MessageResponse("Account confermato con successo. Ora puoi accedere.");
    }

    public AuthResponse loginWithPassword(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Credenziali non valide", HttpStatus.UNAUTHORIZED));

        assertEnabled(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Credenziali non valide", HttpStatus.UNAUTHORIZED);
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public MessageResponse requestLoginOtp(OtpRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Utente non trovato", HttpStatus.NOT_FOUND));

        assertEnabled(user);

        String code = generateNumericCode();
        OtpCode otpCode = OtpCode.builder()
                .code(code)
                .purpose(OtpPurpose.LOGIN)
                .user(user)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();
        otpCodeRepository.save(otpCode);

        emailSimulationService.sendLoginOtpEmail(user.getEmail(), code);

        return new MessageResponse("Codice di accesso inviato alla tua email.");
    }

    @Transactional
    public AuthResponse loginWithOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Credenziali non valide", HttpStatus.UNAUTHORIZED));

        assertEnabled(user);

        OtpCode otpCode = otpCodeRepository
                .findTopByUserAndPurposeAndUsedFalseOrderByCreatedAtDesc(user, OtpPurpose.LOGIN)
                .orElseThrow(() -> new ApiException("Nessun codice richiesto per questo utente", HttpStatus.BAD_REQUEST));

        if (!otpCode.getCode().equals(request.getCode())) {
            throw new ApiException("Codice non valido", HttpStatus.UNAUTHORIZED);
        }

        if (otpCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Codice scaduto, richiedine uno nuovo", HttpStatus.UNAUTHORIZED);
        }

        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);

        return buildAuthResponse(user);
    }

    private void assertEnabled(User user) {
        if (!user.isEnabled()) {
            throw new ApiException("Account non confermato. Controlla la tua email.", HttpStatus.FORBIDDEN);
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getFirstName() + " " + user.getLastName());
    }

    private String generateNumericCode() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
