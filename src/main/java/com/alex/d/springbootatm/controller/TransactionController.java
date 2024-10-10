package com.alex.d.springbootatm.controller;

import com.alex.d.springbootatm.model.BankCardModel;
import com.alex.d.springbootatm.repository.BankCardRepository;
import com.alex.d.springbootatm.response.ErrorResponse;
import com.alex.d.springbootatm.response.TransferResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api/v1")
@Tag(name = "Transactions")
public class TransactionController {

    @Autowired
    private BankCardRepository bankCardRepository;
    @Autowired
    private ATMService atmService;
    @Autowired
    private KafkaProducerService kafkaProducerService;


    @PostMapping("/transfer")
    @Operation(
            description = "Transfer funds between cards",
            summary = "Transfer funds between cards",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = TransferResponse.class))}),
                    @ApiResponse(responseCode = "400", description = "Bad request", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
                    @ApiResponse(responseCode = "404", description = "Not found", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))
                    })
            }
    )
    public ResponseEntity transferFunds(
            @Parameter(description = "Sender card number", required = true) @RequestParam("senderCardNumber") String senderCardNumber,
            @Parameter(description = "Recipient card number", required = true) @RequestParam("recipientCardNumber") String recipientCardNumber,
            @Parameter(description = "Transfer amount", required = true) @RequestParam("amount") BigDecimal amount
    ) {

        Optional<BankCardModel> senderCard = bankCardRepository.findByCardNumber(senderCardNumber);
        Optional<BankCardModel> recipientCard = bankCardRepository.findByCardNumber(recipientCardNumber);

        if (senderCard.isEmpty()) {
            log.error("Card number not found {}", senderCard);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "404", "Card number not found", "/transfer/" + senderCardNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } else if (recipientCard.isEmpty()) {
            log.error("Card number not found {}", recipientCard);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "404", "Card number not found", "/transfer/" + recipientCardNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid transfer amount: {}", amount);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "400", "Invalid deposit amount", "/transfer/" + amount);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        BigDecimal senderBalance = senderCard.get().getBalance();

        if (senderBalance.compareTo(amount) < 0) {
            log.error("Insufficient funds on sender's card: {}", senderBalance);
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "400", "Insufficient funds on sender's card", "/transfer/" + senderBalance);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        BigDecimal recipientBalance = recipientCard.get().getBalance();

        atmService.sendTransaction(senderCard, recipientCard, amount);
        log.info("Transactions of {} from card {} to card {} was successful.", amount, senderCardNumber, recipientCardNumber);
        kafkaProducerService.sendMessage("atm-topic", "Transactions of " + amount + " from card " + senderCardNumber + " to card " + recipientCardNumber + " was successful.");

        TransferResponse response = new TransferResponse(senderCardNumber, recipientCardNumber, amount, senderBalance.subtract(amount), recipientBalance.add(amount));

        return ResponseEntity.ok(response);
    }
}



