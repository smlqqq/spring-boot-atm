package com.alex.d.springbootatm.service;

import com.alex.d.springbootatm.exception.CardNotFoundException;
import com.alex.d.springbootatm.model.ATMModel;
import com.alex.d.springbootatm.model.BankCardModel;
import com.alex.d.springbootatm.repository.ATMRepository;
import com.alex.d.springbootatm.repository.BankCardRepository;
import com.alex.d.springbootatm.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class ATMModelServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankCardRepository bankCardRepository;

    @Mock
    private ATMRepository atmRepository;

    @InjectMocks
    private ATMService atmService;

    @Test
    public void testCheckBalance() throws CardNotFoundException {
        String cardNumber = "1234567890123456";
        BankCardModel bankCardModel = new BankCardModel();
        bankCardModel.setCardNumber(cardNumber);
        bankCardModel.setBalance(BigDecimal.valueOf(1000));
        when(bankCardRepository.findByCardNumber(cardNumber)).thenReturn((bankCardModel));
        BigDecimal balance = atmService.checkBalance(cardNumber);
        assertEquals(BigDecimal.valueOf(1000), balance);
    }

    @Test
    public void testDeleteCardByNumber() throws CardNotFoundException {
        String cardNumber = "1234567890123456";
        BankCardModel bankCardModel = new BankCardModel();
        bankCardModel.setCardNumber(cardNumber);
        when(bankCardRepository.findByCardNumber(cardNumber)).thenReturn((bankCardModel));
        BankCardModel deletedCard = atmService.deleteCardByNumber(cardNumber);
        assertEquals((bankCardModel), deletedCard);
    }

    @Test
    public void testSendTransaction() throws CardNotFoundException {
        BankCardModel senderCard = new BankCardModel();
        senderCard.setBalance(BigDecimal.valueOf(1000));
        BankCardModel recipientCard = new BankCardModel();
        recipientCard.setBalance(BigDecimal.valueOf(500));
        BigDecimal amount = BigDecimal.valueOf(200);
        atmService.sendTransaction((senderCard), (recipientCard), amount);
        verify(bankCardRepository, times(1)).save(senderCard);
        verify(bankCardRepository, times(1)).save(recipientCard);
    }

    @Test
    void testDepositCashFromATM() throws CardNotFoundException {
        BankCardModel card = new BankCardModel(1L, "1234567890123456", "1111", BigDecimal.valueOf(500));
        BigDecimal amount = BigDecimal.valueOf(200);
        List<ATMModel> allATMModels = new ArrayList<>();
        allATMModels.add(new ATMModel(1L,"ATM1","null"));
        allATMModels.add(new ATMModel(2L,"ATM2","null"));
        when(atmRepository.findAll()).thenReturn(allATMModels);
        atmService.depositCashFromATM(card, amount);
        assertEquals(BigDecimal.valueOf(700), card.getBalance());
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void testWithdrawFromATM() throws CardNotFoundException {
        BankCardModel card = new BankCardModel(1L, "1234567890123456", "1111", BigDecimal.valueOf(500));
        BigDecimal amount = BigDecimal.valueOf(200);
        List<ATMModel> allATMModels = new ArrayList<>();
        allATMModels.add(new ATMModel(1L,"ATM1","null"));
        when(atmRepository.findAll()).thenReturn(allATMModels);
        atmService.withdrawFromATM(card, amount);
        assertEquals(BigDecimal.valueOf(300), card.getBalance());
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void testCreateCard() {
        BankCardModel newCard = new BankCardModel(1L, "1234567890123456", "1111", BigDecimal.valueOf(0));
        when(bankCardRepository.save(any())).thenReturn(newCard);
        BankCardModel createdCard = atmService.createCard();
        assertNotNull(createdCard);
        assertEquals("1234567890123456", createdCard.getCardNumber());
        assertEquals("1111", createdCard.getPinNumber());
        assertEquals(BigDecimal.valueOf(0), createdCard.getBalance());
    }
}
