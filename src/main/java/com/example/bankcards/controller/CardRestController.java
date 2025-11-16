package com.example.bankcards.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cards")
public class CardRestController {

    private final CardService cardService;

    public CardRestController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> createCard(@PathVariable Long userId, @Valid @RequestBody CardRequest cardRequest) {
        CardDto createdCard = cardService.createCard(userId, cardRequest);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCard.id())
                .toUri();
        return ResponseEntity.created(location).body(createdCard);

    }

    @PutMapping("/{cardId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> updateCardStatus(@PathVariable UUID cardId, @RequestParam CardStatus cardStatus) {
        CardDto updatedCard = cardService.updateCardStatus(cardId, cardStatus);
        return ResponseEntity.ok().body(updatedCard);
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Page<CardDto>> getUserCards(@PathVariable Long userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<CardDto> cards = cardService.getUserCards(userId, pageable);
        return ResponseEntity.ok().body(cards);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> transfer(
            @RequestParam UUID fromCardId,
            @RequestParam UUID toCardId,
            @RequestParam BigDecimal amount) {
        cardService.transfer(fromCardId, toCardId, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CardDto> getCardById(@PathVariable UUID cardId) {
        CardDto card = cardService.getCard(cardId);
        return ResponseEntity.ok().body(card);
    }

}
