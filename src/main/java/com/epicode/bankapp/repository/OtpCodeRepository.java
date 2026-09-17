package com.epicode.bankapp.repository;

import com.epicode.bankapp.model.OtpCode;
import com.epicode.bankapp.model.OtpPurpose;
import com.epicode.bankapp.model.Transfer;
import com.epicode.bankapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByUserAndPurposeAndUsedFalseOrderByCreatedAtDesc(User user, OtpPurpose purpose);

    Optional<OtpCode> findTopByTransferAndPurposeAndUsedFalseOrderByCreatedAtDesc(Transfer transfer, OtpPurpose purpose);
}
