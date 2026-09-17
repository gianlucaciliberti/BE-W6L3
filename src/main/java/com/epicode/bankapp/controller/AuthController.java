package com.epicode.bankapp.controller;

import com.epicode.bankapp.dto.request.LoginRequest;
import com.epicode.bankapp.dto.request.OtpRequestDto;
import com.epicode.bankapp.dto.request.OtpVerifyRequest;
import com.epicode.bankapp.dto.request.RegisterRequest;
import com.epicode.bankapp.dto.response.AuthResponse;
import com.epicode.bankapp.dto.response.MessageResponse;
import com.epicode.bankapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/confirm")
    public MessageResponse confirm(@RequestParam String token) {
        return authService.confirmAccount(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.loginWithPassword(request);
    }

    @PostMapping("/login/otp/request")
    public MessageResponse requestLoginOtp(@Valid @RequestBody OtpRequestDto request) {
        return authService.requestLoginOtp(request);
    }

    @PostMapping("/login/otp/verify")
    public AuthResponse verifyLoginOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.loginWithOtp(request);
    }
}
