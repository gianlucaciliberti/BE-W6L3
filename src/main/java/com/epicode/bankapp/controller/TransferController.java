package com.epicode.bankapp.controller;

import com.epicode.bankapp.dto.request.TransferConfirmRequest;
import com.epicode.bankapp.dto.request.TransferRequest;
import com.epicode.bankapp.dto.response.TransferResponse;
import com.epicode.bankapp.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public TransferResponse createTransfer(@AuthenticationPrincipal UserDetails principal,
                                            @Valid @RequestBody TransferRequest request) {
        return transferService.createTransfer(principal.getUsername(), request);
    }

    @PostMapping("/{id}/confirm")
    public TransferResponse confirmTransfer(@AuthenticationPrincipal UserDetails principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody TransferConfirmRequest request) {
        return transferService.confirmTransfer(principal.getUsername(), id, request);
    }
}
