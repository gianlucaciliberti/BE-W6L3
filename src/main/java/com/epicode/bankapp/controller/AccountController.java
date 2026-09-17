package com.epicode.bankapp.controller;

import com.epicode.bankapp.dto.response.AccountResponse;
import com.epicode.bankapp.exception.ApiException;
import com.epicode.bankapp.model.User;
import com.epicode.bankapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ApiException("Utente non trovato", HttpStatus.NOT_FOUND));

        return new AccountResponse(user.getEmail(), user.getFirstName() + " " + user.getLastName(), user.getBalance());
    }
}
