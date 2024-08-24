package com.alex.d.springbootatm.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "atm-topic", groupId = "consumer")
    public void listen(String message) {
        System.out.println("Received message: " + message);
    }
}
