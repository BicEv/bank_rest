package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;

public class CardServiceTest {

    private CardRepository cardRepository;
    private UserRepository userRepository;
    private CardService cardService;

    private User admin;
    private User user;
    private User otherUser;
    private Card card;

    @BeforeEach
    void init() {
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        cardService = new CardService(userRepository, cardRepository);

        admin = User.builder()
                .id(1L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        user = User.builder()
                .id(10L)
                .username("regular_user")
                .fullName("John Doe")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(999L)
                .username("other_user")
                .fullName("Jane Doe")
                .role(Role.USER)
                .build();

        card = new Card();
        card.setId(UUID.randomUUID());
        card.setOwner(user);
        card.setPlainNumber("1234567887654321");
        card.setExpiryMonth(10);
        card.setExpiryYear(2030);
        card.setBalance(BigDecimal.valueOf(100));
        card.setStatus(CardStatus.ACTIVE);
    }

    private void authenticateAs(User user) {
        CustomUserDetails cud = new CustomUserDetails(user);
        TestingAuthenticationToken auth = new TestingAuthenticationToken(cud, null, "ROLE_" + user.getRole().name());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private CardRequest getCardRequest(String plainNumber, BigDecimal balance) {
        return new CardRequest(plainNumber, 2030, 10, balance);
    }

    private Card createCard(User owner, String number, BigDecimal balance, CardStatus status) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setOwner(owner);
        card.setPlainNumber(number);
        card.setStatus(status);
        card.setBalance(balance);
        card.setExpiryMonth(9);
        card.setExpiryYear(2029);
        return card;

    }

    // Create card
    @Test
    void createCard_success() {
        authenticateAs(admin);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> {
            Card saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var request = getCardRequest("1234567887654321", BigDecimal.valueOf(50));

        CardDto result = cardService.createCard(10L, request);

        assertEquals("John Doe", result.ownerFullName());
        assertEquals(BigDecimal.valueOf(50).setScale(2), result.balance());
        verify(cardRepository, times(1)).save(any(Card.class));

    }

    @Test
    void createCard_asUser_forbidden() {
        authenticateAs(user);

        var request = getCardRequest("1234567887654321", BigDecimal.valueOf(100));

        assertThrows(SecurityException.class, () -> cardService.createCard(10L, request));
    }

    // Get card
    @Test
    void getCard_asOwner_success() {
        authenticateAs(user);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        var result = cardService.getCard(card.getId());

        assertEquals(card.getId(), result.id());
        assertTrue(result.maskedNumber().contains(card.getLast4()));
        assertEquals(card.getBalance(), result.balance());
    }

    @Test
    void getCard_asOtherUser_forbidden() {
        authenticateAs(otherUser);
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        assertThrows(SecurityException.class, () -> cardService.getCard(card.getId()));
    }

    @Test
    void getCard_asAdmin_success() {
        authenticateAs(admin);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        var result = cardService.getCard(card.getId());

        assertEquals(card.getId(), result.id());
        assertTrue(result.maskedNumber().contains(card.getLast4()));
        assertEquals(card.getBalance(), result.balance());
    }

    @Test
    void getCard_cardNotFound() {
        authenticateAs(admin);

        when(cardRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.getCard(UUID.randomUUID()));
    }

    // Get user's cards
    @Test
    void getUsersCards_success() {
        authenticateAs(user);

        Pageable pageable = PageRequest.of(0, 10);
        List<Card> cards = List.of(
                createCard(user, "8000700060005000", BigDecimal.valueOf(1500), CardStatus.ACTIVE),
                createCard(user, "1000200030004000", BigDecimal.ZERO, CardStatus.ACTIVE));
        Page<Card> page = new PageImpl<>(cards, pageable, cards.size());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cardRepository.findByOwner(user, pageable)).thenReturn(page);

        Page<CardDto> result = cardService.getUserCards(user.getId(), pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("**** **** **** 5000", result.getContent().get(0).maskedNumber());
    }

    @Test
    void getUsersCards_asAdmin_success() {
        authenticateAs(admin);

        Pageable pageable = PageRequest.of(0, 10);
        List<Card> cards = List.of(
                createCard(user, "8000700060005000", BigDecimal.valueOf(1500), CardStatus.ACTIVE),
                createCard(user, "1000200030004000", BigDecimal.ZERO, CardStatus.ACTIVE));
        Page<Card> page = new PageImpl<>(cards, pageable, cards.size());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cardRepository.findByOwner(user, pageable)).thenReturn(page);

        Page<CardDto> result = cardService.getUserCards(user.getId(), pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("**** **** **** 5000", result.getContent().get(0).maskedNumber());

    }

    @Test
    void getUsersCards_byAnother_forbidden() {
        authenticateAs(otherUser);

        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(SecurityException.class, () -> cardService.getUserCards(user.getId(), pageable));
    }

    // Update card status
    @Test
    void updateCardStatus_success() {
        authenticateAs(admin);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CardDto result = cardService.updateCardStatus(card.getId(), CardStatus.BLOCKED);

        assertEquals(CardStatus.BLOCKED, result.cardStatus());
    }

    @Test
    void updateCardStatus_asUser_forbidden() {
        authenticateAs(user);

        assertThrows(SecurityException.class,
                () -> cardService.updateCardStatus(card.getId(), CardStatus.BLOCKED));
    }

    // Transfer
    @Test
    void transffer_success() {
        authenticateAs(user);

        Card from = createCard(user, "8000700060005000", BigDecimal.valueOf(1500), CardStatus.ACTIVE);
        Card to = createCard(user, "1000200030004000", BigDecimal.ZERO, CardStatus.ACTIVE);

        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));
        when(cardRepository.findById(to.getId())).thenReturn(Optional.of(to));

        cardService.transfer(from.getId(), to.getId(), BigDecimal.valueOf(500));

        assertEquals(BigDecimal.valueOf(1000).setScale(2), from.getBalance());
        assertEquals(BigDecimal.valueOf(500).setScale(2), to.getBalance());

        verify(cardRepository).saveAll(any());

    }

    @Test
    void transffer_insufficientFunds() {
        authenticateAs(user);

        Card from = createCard(user, "8000700060005000", BigDecimal.valueOf(1500), CardStatus.ACTIVE);
        Card to = createCard(user, "1000200030004000", BigDecimal.ZERO, CardStatus.ACTIVE);

        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));
        when(cardRepository.findById(to.getId())).thenReturn(Optional.of(to));

        assertThrows(InsufficientFundsException.class,
                () -> cardService.transfer(from.getId(), to.getId(), BigDecimal.valueOf(1500.1)));
    }

    @Test
    void transfer_negativeAmount() {
        authenticateAs(user);

        assertThrows(IllegalArgumentException.class,
                () -> cardService.transfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(-1)));
    }

    @Test
    void transfer_otherUsersCard_forbidden() {
        authenticateAs(otherUser);

        Card from = createCard(user, "8000700060005000", BigDecimal.valueOf(1500), CardStatus.ACTIVE);
        Card to = createCard(user, "1000200030004000", BigDecimal.ZERO, CardStatus.ACTIVE);

        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));
        when(cardRepository.findById(to.getId())).thenReturn(Optional.of(to));

        assertThrows(SecurityException.class,
                () -> cardService.transfer(from.getId(), to.getId(), BigDecimal.valueOf(50)));

    }

    // Delete card
    @Test
    void deleteCard_success() {
        authenticateAs(admin);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        cardService.deleteCard(card.getId());

        verify(cardRepository, times(1)).delete(card);
    }

    @Test
    void deleteCard_asUser_forbidden() {
        authenticateAs(user);

        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        assertThrows(SecurityException.class, () -> cardService.deleteCard(card.getId()));
    }

}
