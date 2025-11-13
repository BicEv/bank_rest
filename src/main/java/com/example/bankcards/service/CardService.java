package com.example.bankcards.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;

@Service
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public CardService(UserRepository userRepository, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public CardDto createCard(Long userId, CardRequest cardRequest) {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser))
            throw new SecurityException("Only admin can create cards");

        User owner = getUserOrThrow(userId);

        Card card = new Card();
        card.setOwner(owner);
        card.setPlainNumber(cardRequest.plainNumber());
        card.setExpiryYear(cardRequest.expiryYear());
        card.setExpiryMonth(cardRequest.expiryMonth());
        card.setBalance(
                cardRequest.initialBalance() != null ? cardRequest.initialBalance().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
        card.setStatus(CardStatus.ACTIVE);

        Card savedCard = cardRepository.save(card);
        logger.debug("Created card: {}", savedCard.getId().toString());
        return toDto(savedCard);
    }

    public CardDto getCard(UUID cardId) {
        User currentUser = getCurrentUser();

        Card foundCard = getCardOrThrow(cardId);

        if (!isAdmin(currentUser) && !foundCard.getOwner().getId().equals(currentUser.getId()))
            throw new SecurityException("Access denied: card doesn't belong to you");

        logger.debug("Card retrieved: {}", foundCard.getId().toString());
        return toDto(foundCard);
    }

    public Page<CardDto> getUserCards(Long userId, Pageable pageable) {
        User currentUser = getCurrentUser();
        User owner = getUserOrThrow(userId);
        if (!currentUser.getId().equals(owner.getId()) && !isAdmin(currentUser))
            throw new SecurityException("Access denied");
        logger.debug("Page of card retrieved for user: {}", userId);
        Page<Card> cards = cardRepository.findByOwner(owner, pageable);
        return cards.map(this::toDto);
    }

    @Transactional
    public CardDto updateCardStatus(UUID cardId, CardStatus cardStatus) {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser))
            throw new SecurityException("Access denied");

        Card foundCard = getCardOrThrow(cardId);
        foundCard.setStatus(cardStatus);
        Card updatedCard = cardRepository.save(foundCard);
        logger.debug("Card was updated: {}", updatedCard.getId().toString());
        return toDto(updatedCard);
    }

    @Transactional
    public void transfer(UUID fromCardId, UUID toCardId, BigDecimal amount, Long userId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Transfer amount must be positive");

        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(userId))
            throw new SecurityException("Access denied");

        Card fromCard = getCardOrThrow(fromCardId);
        Card toCard = getCardOrThrow(toCardId);

        if (!fromCard.getOwner().getId().equals(userId) || !toCard.getOwner().getId().equals(userId)) {
            throw new SecurityException("You can transfer only between your own cards");
        }

        if (fromCard.getStatus() != CardStatus.ACTIVE || toCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Both cards must be active");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        fromCard.setBalance(fromCard.getBalance().subtract(scaledAmount));
        toCard.setBalance(toCard.getBalance().add(scaledAmount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
        logger.debug("Transferred {} from card {} to card {}", scaledAmount, fromCardId, toCardId);
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser))
            throw new SecurityException("Access denied");

        Card card = getCardOrThrow(cardId);
        cardRepository.delete(card);
        logger.debug("Card was deleted: {}", cardId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found, id: " + userId));
    }

    private Card getCardOrThrow(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found, id: " + cardId.toString()));
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        return userDetails.getUser();
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private CardDto toDto(Card card) {
        return new CardDto(
                card.getId(),
                card.getMaskedNumber(),
                card.getOwner().getFullName(),
                card.getExpiryYear(),
                card.getExpiryMonth(),
                card.getStatus(),
                card.getBalance());
    }

}
