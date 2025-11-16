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
import com.example.bankcards.dto.ErrorResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cards")
public class CardRestController {

    private final CardService cardService;

    public CardRestController(CardService cardService) {
        this.cardService = cardService;
    }

    @Operation(summary = "Создать новую карту для пользователя", description = "Создает новую карту для казанного пользователя. Доступно только для админов.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Карта успешно создана", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> createCard(@PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Данные новой карты", required = true, content = @Content(schema = @Schema(implementation = CardRequest.class))) @Valid @RequestBody CardRequest cardRequest) {
        CardDto createdCard = cardService.createCard(userId, cardRequest);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCard.id())
                .toUri();
        return ResponseEntity.created(location).body(createdCard);

    }

    @Operation(summary = "Обновить статус карты", description = "Обновляет статус карты (например, BLOCKED, ACTIVE). Доступно только для админов.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус карты успешно обновлен", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Карта не найдена", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{cardId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> updateCardStatus(@PathVariable UUID cardId, @RequestParam CardStatus cardStatus) {
        CardDto updatedCard = cardService.updateCardStatus(cardId, cardStatus);
        return ResponseEntity.ok().body(updatedCard);
    }

    @Operation(summary = "Удалить карту", description = "Удаляет карту по идентификатору. Доступно только для админов.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Карта успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить карты пользователя", description = "Возвращает постраничный список карт указанного пользователя. Доступно для админа и пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список карт получен", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Page<CardDto>> getUserCards(@PathVariable Long userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<CardDto> cards = cardService.getUserCards(userId, pageable);
        return ResponseEntity.ok().body(cards);
    }

    @Operation(summary = "Перевести деньги между картами", description = "Перевод средств с одной карты на другую. Доступно для админа и пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перевод выполнен успешно"),
            @ApiResponse(responseCode = "400", description = "Ошибка при переводе (например, недостаточно средств)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> transfer(
            @RequestParam UUID fromCardId,
            @RequestParam UUID toCardId,
            @RequestParam BigDecimal amount) {
        cardService.transfer(fromCardId, toCardId, amount);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получить карту по ID", description = "Возвращает информацию о карте по её идентификатору. Доступно для админа и пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта найдена", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Карта не найдена", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CardDto> getCardById(@PathVariable UUID cardId) {
        CardDto card = cardService.getCard(cardId);
        return ResponseEntity.ok().body(card);
    }

}
