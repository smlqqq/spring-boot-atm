package com.alex.d.springbootatm.model;


import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

}

