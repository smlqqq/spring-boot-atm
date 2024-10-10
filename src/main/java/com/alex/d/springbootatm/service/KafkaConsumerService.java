package com.alex.d.springbootatm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class KafkaConsumerService {

    @KafkaListener(topics = {"atm-topic","personal-data-topic"}, groupId = "consumer")
    public void listen(String message) {
        log.info("Kafka message : {}", message);
    }

}
