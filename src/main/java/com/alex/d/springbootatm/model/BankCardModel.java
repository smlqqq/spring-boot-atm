package com.alex.d.springbootatm.model;


import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "atm_accounts")
@JsonTypeName("Bank card")
public class BankCardModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Schema(description = "id")
    private Long id;

    @Column(name = "card_number")
    @Schema(description = "card number", example = "4000006819910091")
    private String cardNumber;

    @Column(name = "pin_number")
    @Schema(description = "card pin code", example = "5492")
    private String pinNumber;

    @Column(name = "balance")
    @Schema(description = "card balance", example = "100")
    private BigDecimal balance;

    @OneToMany(mappedBy = "senderCard") // Field senderCard in TransactionModel
    private Set<TransactionModel> sentTransactions;

    @OneToMany(mappedBy = "recipientCard") // Field recipientCard in TransactionModel
    private Set<TransactionModel> receivedTransactions;

    public BankCardModel(Long id, String cardNumber, String pinNumber, BigDecimal balance) {
        this.id = id;
        this.cardNumber = cardNumber;
        this.pinNumber = pinNumber;
        this.balance = balance;
    }

}

