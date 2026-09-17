package com.epicode.bankapp.repository;

import com.epicode.bankapp.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
