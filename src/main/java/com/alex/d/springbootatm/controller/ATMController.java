package com.alex.d.springbootatm.controller;

import com.alex.d.springbootatm.model.BankCardModel;
import com.alex.d.springbootatm.repository.BankCardRepository;
import com.alex.d.springbootatm.response.BalanceResponse;
import com.alex.d.springbootatm.response.DepositResponse;
import com.alex.d.springbootatm.response.ErrorResponse;
import com.alex.d.springbootatm.response.WithdrawResponse;
import com.alex.d.springbootatm.service.ATMService;
import com.alex.d.springbootatm.service.KafkaProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Controller
@Slf4j
@RequestMapping("/api/v1")
@Tag(name = "Bank card")
public class ATMController {

    @Autowired
    private ATMService atmService;
    @Autowired
    private BankCardRepository bankCardRepository;
    @Autowired
    private KafkaProducerService kafkaProducerService;


    @Operation(
            summary = "Get account balance.",
            description = "Returns the balance for the provided card number.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = BalanceResponse.class))}),
                    @ApiResponse(responseCode = "400", description = "Bad request", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
                    @ApiResponse(responseCode = "404", description = "Not found", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))
                    })
            }
    )
    @GetMapping("/balance/{cardNumber}")
    public ResponseEntity getBalance(@PathVariable String cardNumber) {

        Optional<BankCardModel> card = bankCardRepository.findByCardNumber(cardNumber);
        if (card.isEmpty()) {
            log.error("Card not found for number: {}", cardNumber);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "404", "Card not found", "/withdraw/" + cardNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        BigDecimal balance = atmService.checkBalanceByCardNumber(cardNumber);
        log.info("Card: {} Balance: {}", cardNumber, balance);
        kafkaProducerService.sendMessage("atm-topic", "Card: " + cardNumber + " Balance: " + balance);

        BalanceResponse response = new BalanceResponse(cardNumber, balance);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }


    @Operation(
            summary = "Deposit funds to the specified card",
            description = "Deposit funds to the specified card using ATM and amount",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = DepositResponse.class))}),
                    @ApiResponse(responseCode = "400", description = "Bad request", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
                    @ApiResponse(responseCode = "404", description = "Not found", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
            }
    )
    @PutMapping("/deposit")
    public ResponseEntity depositCash(
            @Parameter(description = "Recipient card number", required = true) @RequestParam("cardNumber") String cardNumber,
            @Parameter(description = "Amount to deposit", required = true) @RequestParam("amount") BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid deposit amount: {}", amount);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "400", "Invalid deposit amount", "/deposit/" + amount);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Optional<BankCardModel> card = bankCardRepository.findByCardNumber(cardNumber);
        if (card.isEmpty()) {
            log.error("Card not found for number: {}", cardNumber);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "404", "Card not found", "/withdraw/" + cardNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }


        BigDecimal recipientCardBalance = card.get().getBalance();
        atmService.depositCashFromATM(card, amount);
        log.info("Deposit of {} to card {} was successful.", amount, cardNumber);
        kafkaProducerService.sendMessage("atm-topic", "Deposit of " + amount + " to card " + cardNumber + " successful.");

        DepositResponse depositResponse = new DepositResponse(cardNumber, recipientCardBalance.add(amount));
        return ResponseEntity.status(HttpStatus.OK).body(depositResponse);

    }


    @Operation(
            summary = "Withdraw funds from ATM",
            description = "Withdraw funds from the specified card using the provided card number and amount",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = WithdrawResponse.class))}),
                    @ApiResponse(responseCode = "400", description = "Bad request", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
                    @ApiResponse(responseCode = "404", description = "Not found", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))
                    })
            }
    )
    @PostMapping("/withdraw")
    public ResponseEntity withdraw(
            @Parameter(description = "Card number", required = true) @RequestParam("cardNumber") String cardNumber,
            @Parameter(description = "Amount to withdraw", required = true) @RequestParam("amount") BigDecimal amount) {

        Optional<BankCardModel> card = bankCardRepository.findByCardNumber(cardNumber);
        if (card.isEmpty()) {
            log.error("Card not found for number: {}", cardNumber);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "404", "Card not found", "/withdraw/" + cardNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        BigDecimal balance = card.get().getBalance();
        if (balance.compareTo(amount) <= 0) {
            log.error("Insufficient funds on your card: {}", card);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "400", "Failed to withdraw funds. " + "balance " + balance, "/withdraw/" + cardNumber);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid deposit amount: {}", amount);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "400", "Negative amount, or equals to zero", "/withdraw/" + amount);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }


        atmService.withdrawFromATM(card, amount);
        log.info("Withdrawal of {} from card {} was successful.", amount, cardNumber);
        kafkaProducerService.sendMessage("atm-topic", "Withdrawal of " + amount + " from card " + cardNumber + " was successful.");

        WithdrawResponse response = new WithdrawResponse(cardNumber, amount.toString(), balance.subtract(amount));
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

}
