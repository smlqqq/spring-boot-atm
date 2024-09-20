package com.alex.d.springbootatm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankCardDTO {
    private String cardNumber;
    private String pinCode; // Оригинальный 4-значный PIN-код
    private BigDecimal balance;
}
