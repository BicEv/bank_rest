package com.example.bankcards.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Convert()
    @Column(name = "number_encrypted", length = 4096)
    private String number;

    @Column(name = "last4", length = 4, nullable = false)
    private String last4;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    public void setPlainNumber(String plainNumber) {
        this.number = plainNumber;
        if (plainNumber != null && plainNumber.length() >= 4) {
            this.last4 = plainNumber.substring(plainNumber.length() - 4);
        }
    }

    public String getMaskedNumber() {
        return "**** **** **** " + last4;
    }

}
