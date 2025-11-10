package com.example.bankcards.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;

public interface CardRepository extends JpaRepository<Card, UUID> {

    Page<Card> findByOwner(User owner, Pageable pageable);

    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);

    Page<Card> findByIdAndOwner(User owner, UUID id, Pageable pageable);

    List<Card> findByOwnerAndLast4(User owner, String last4);

}
