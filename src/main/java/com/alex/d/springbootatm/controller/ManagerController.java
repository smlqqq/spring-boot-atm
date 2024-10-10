package com.alex.d.springbootatm.controller;

import com.alex.d.springbootatm.dto.BankCardDTO;
import com.alex.d.springbootatm.service.KafkaProducerService;
import com.alex.d.springbootatm.model.BankCardModel;
import com.alex.d.springbootatm.repository.BankCardRepository;
import com.alex.d.springbootatm.response.ErrorResponse;
import com.alex.d.springbootatm.service.ATMService;
import com.alex.d.springbootatm.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Manager")
public class ManagerController {

    @Autowired
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private BankCardRepository bankCardRepository;
    @Autowired
    private ATMService atmService;
    @Autowired
    private ReportService reportService;


    @Operation(
            summary = "Get all bank cards",
            description = "Retrieve details of all bank cards",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = BankCardModel.class))
                    })
            }
    )
    @GetMapping("/cards")
    public ResponseEntity<List<BankCardModel>> getAllCards() {
        List<BankCardModel> cards = bankCardRepository.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(cards);
    }

    @Operation(
            summary = "Delete card",
            description = "Delete all details about card",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = BankCardModel.class))}),
                    @ApiResponse(responseCode = "400", description = "Bad request", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
                    @ApiResponse(responseCode = "404", description = "Not found", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
            }
    )
    @DeleteMapping("/delete/{cardNumber}")
    public ResponseEntity deleteCard(@PathVariable("cardNumber") String cardNumber) {
        
        if (!cardNumber.isEmpty()) {
                atmService.deleteCardByNumber(cardNumber);
                log.info("Card with number {} was deleted", cardNumber);
                kafkaProducerService.sendMessage("atm-topic", "Card with number " + cardNumber + " was deleted");
                return ResponseEntity.status(HttpStatus.OK).body(cardNumber);
        } else

            log.error("Invalid credit card number {}", cardNumber);
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), "404", "Card not found", "/delete/" + cardNumber);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @Operation(
            summary = "Create new bank card",
            description = "Create a new bank card using the provided details",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = BankCardModel.class))
                    })
            }

    )

    @PostMapping("/card")
    public ResponseEntity createNewCard() {
        BankCardDTO createdCard = atmService.createCard();
        log.info("New card created: {}", createdCard.getCardNumber());
        kafkaProducerService.sendMessage("atm-topic", "New card created: " + createdCard.getCardNumber() + " " + createdCard.getPinCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }


    @Operation(
            summary = "Download",
            description = "Download all data.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")}),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
            }
    )
    @GetMapping("/download")
    public ResponseEntity downloadFile() {
        return reportService.generateClientReport();
    }

    @Operation(
            summary = "Download",
            description = "Download data by a card number.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success", content = {
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")}),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = {
                            @Content(mediaType = "application/json;charset=UTF-8", schema = @Schema(implementation = ErrorResponse.class))}),
            }
    )

    @GetMapping("/download/{cardNumber}")
    public ResponseEntity downloadDataByACardNumber(@PathVariable("cardNumber") String cardNumber) {
        return reportService.generateIndividualClientReport(cardNumber);
    }
}
