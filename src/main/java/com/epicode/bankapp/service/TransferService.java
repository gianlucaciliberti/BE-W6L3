package com.epicode.bankapp.service;

import com.epicode.bankapp.dto.request.TransferConfirmRequest;
import com.epicode.bankapp.dto.request.TransferRequest;
import com.epicode.bankapp.dto.response.TransferResponse;
import com.epicode.bankapp.exception.ApiException;
import com.epicode.bankapp.model.OtpCode;
import com.epicode.bankapp.model.OtpPurpose;
import com.epicode.bankapp.model.Transfer;
import com.epicode.bankapp.model.TransferStatus;
import com.epicode.bankapp.model.User;
import com.epicode.bankapp.repository.OtpCodeRepository;
import com.epicode.bankapp.repository.TransferRepository;
import com.epicode.bankapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final EmailSimulationService emailSimulationService;

    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    @Transactional
    public TransferResponse createTransfer(String senderEmail, TransferRequest request) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ApiException("Utente non trovato", HttpStatus.NOT_FOUND));

        if (senderEmail.equalsIgnoreCase(request.getRecipientEmail())) {
            throw new ApiException("Non puoi effettuare un bonifico verso te stesso", HttpStatus.BAD_REQUEST);
        }

        User recipient = userRepository.findByEmail(request.getRecipientEmail())
                .orElseThrow(() -> new ApiException("Destinatario non trovato", HttpStatus.NOT_FOUND));

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ApiException("Saldo insufficiente", HttpStatus.BAD_REQUEST);
        }

        Transfer transfer = Transfer.builder()
                .sender(sender)
                .recipient(recipient)
                .amount(request.getAmount())
                .status(TransferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        transferRepository.save(transfer);

        String code = generateNumericCode();
        OtpCode otpCode = OtpCode.builder()
                .code(code)
                .purpose(OtpPurpose.TRANSFER)
                .user(sender)
                .transfer(transfer)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();
        otpCodeRepository.save(otpCode);

        emailSimulationService.sendTransferOtpEmail(sender.getEmail(), code, transfer.getAmount(), recipient.getEmail());

        return toResponse(transfer);
    }

    @Transactional
    public TransferResponse confirmTransfer(String senderEmail, Long transferId, TransferConfirmRequest request) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ApiException("Bonifico non trovato", HttpStatus.NOT_FOUND));

        if (!transfer.getSender().getEmail().equalsIgnoreCase(senderEmail)) {
            throw new ApiException("Non sei autorizzato a confermare questo bonifico", HttpStatus.FORBIDDEN);
        }

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new ApiException("Il bonifico non è più in sospeso", HttpStatus.BAD_REQUEST);
        }

        OtpCode otpCode = otpCodeRepository
                .findTopByTransferAndPurposeAndUsedFalseOrderByCreatedAtDesc(transfer, OtpPurpose.TRANSFER)
                .orElseThrow(() -> new ApiException("Nessun codice trovato per questo bonifico", HttpStatus.BAD_REQUEST));

        if (!otpCode.getCode().equals(request.getCode())) {
            throw new ApiException("Codice non valido", HttpStatus.UNAUTHORIZED);
        }

        if (otpCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            transfer.setStatus(TransferStatus.EXPIRED);
            transferRepository.save(transfer);
            throw new ApiException("Codice scaduto, il bonifico è stato annullato", HttpStatus.UNAUTHORIZED);
        }

        User sender = transfer.getSender();
        User recipient = transfer.getRecipient();

        if (sender.getBalance().compareTo(transfer.getAmount()) < 0) {
            transfer.setStatus(TransferStatus.CANCELLED);
            transferRepository.save(transfer);
            throw new ApiException("Saldo insufficiente, il bonifico è stato annullato", HttpStatus.BAD_REQUEST);
        }

        sender.setBalance(sender.getBalance().subtract(transfer.getAmount()));
        recipient.setBalance(recipient.getBalance().add(transfer.getAmount()));
        userRepository.save(sender);
        userRepository.save(recipient);

        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);

        transfer.setStatus(TransferStatus.CONFIRMED);
        transfer.setConfirmedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        return toResponse(transfer);
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSender().getEmail(),
                transfer.getRecipient().getEmail(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getConfirmedAt()
        );
    }

    private String generateNumericCode() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
