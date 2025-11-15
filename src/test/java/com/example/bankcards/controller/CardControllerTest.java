package com.example.bankcards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.bankcards.TestSecurityConfig;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CardRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    private CardDto sampleCard() {
        return new CardDto(UUID.randomUUID(), "**** **** **** 1234", "John Doe", 2030, 10, CardStatus.ACTIVE,
                BigDecimal.valueOf(100).setScale(2));
    }

    private CardRequest sampleRequest() {
        return new CardRequest("1234567887654321", 2030, 10, BigDecimal.valueOf(100).setScale(2));
    }

    // Create card
    @Test
    void createCard_success() throws Exception {
        var createdCard = sampleCard();
        var request = sampleRequest();

        when(cardService.createCard(1L, request)).thenReturn(createdCard);

        mockMvc.perform(post("/api/cards/user/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/cards/user/1/" + createdCard.id()))
                .andExpect(jsonPath("$.maskedNumber").value(createdCard.maskedNumber()))
                .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    void createCard_validationError() throws Exception {
        var invalidRequest = new CardRequest("123", 2030, 12, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/cards/user/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].statusCode").value(400))
                .andExpect(jsonPath("$[0].message").value("plainNumber: Card number must be exactly 16 digits long"));
    }

    @Test
    void createCard_userNotFound() throws Exception {
        var request = sampleRequest();

        when(cardService.createCard(1L, request)).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(post("/api/cards/user/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // Get card by id
    @Test
    void getCardById_success() throws Exception {
        var card = sampleCard();
        when(cardService.getCard(card.id())).thenReturn(card);

        mockMvc.perform(get("/api/cards/" + card.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedNumber").value(card.maskedNumber()))
                .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    void getCardById_notFound() throws Exception {
        var id = UUID.randomUUID();
        when(cardService.getCard(id)).thenThrow(new CardNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // Get user's cards
    @Test
    void getUserCards_success() throws Exception {
        Page<CardDto> page = new PageImpl<>(List.of(sampleCard(), sampleCard()), PageRequest.of(0, 10), 2);

        when(cardService.getUserCards(anyLong(), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/cards/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.content[0].ownerFullName").value("John Doe"))
                .andExpect(jsonPath("$.content[0].balance").value(100));
    }

    @Test
    void getUserCards_userNotFound() throws Exception {
        when(cardService.getUserCards(anyLong(), any(PageRequest.class)))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/cards/user/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // Update card status
    @Test
    void updateCardStatus_success() throws Exception {
        var updated = sampleCard();
        updated = new CardDto(updated.id(), updated.maskedNumber(), updated.ownerFullName(), updated.expiryYear(),
                updated.expiryMonth(), CardStatus.BLOCKED, BigDecimal.valueOf(100));

        when(cardService.updateCardStatus(updated.id(), CardStatus.BLOCKED)).thenReturn(updated);

        mockMvc.perform(put("/api/cards/" + updated.id() + "/status")
                .param("cardStatus", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("BLOCKED"));
    }

    @Test
    void updateCardStatus_notFound() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.updateCardStatus(cardId, CardStatus.BLOCKED))
                .thenThrow(new CardNotFoundException("Card not found"));

        mockMvc.perform(put("/api/cards/" + cardId + "/status")
                .param("cardStatus", "BLOCKED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // Delete card
    @Test
    void deleteCard_success() throws Exception {
        var id = UUID.randomUUID();
        mockMvc.perform(delete("/api/cards/" + id))
                .andExpect(status().isNoContent());

        verify(cardService).deleteCard(id);
    }

    // Transfer
    @Test
    void transfer_success() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        mockMvc.perform(post("/api/cards/transfer")
                .param("fromCardId", fromId.toString())
                .param("toCardId", toId.toString())
                .param("amount", "50"))
                .andExpect(status().isOk());

        verify(cardService).transfer(fromId, toId, BigDecimal.valueOf(50));
    }

    @Test
    void transfer_insufficientFunds() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        doThrow(new InsufficientFundsException("Not enough funds"))
                .when(cardService).transfer(fromId, toId, BigDecimal.valueOf(500));

        mockMvc.perform(post("/api/cards/transfer")
                .param("fromCardId", fromId.toString())
                .param("toCardId", toId.toString())
                .param("amount", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough funds"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void transfer_negativeAmount() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Transfer amount must be positive"))
                .when(cardService).transfer(fromId, toId, BigDecimal.valueOf(-1));

        mockMvc.perform(post("/api/cards/transfer")
                .param("fromCardId", fromId.toString())
                .param("toCardId", toId.toString())
                .param("amount", "-1"))
                .andExpect(status().isBadRequest());
    }
}
