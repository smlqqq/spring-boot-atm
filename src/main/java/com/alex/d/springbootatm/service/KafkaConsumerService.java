//package com.alex.d.springbootatm.service;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//@Service
//public class KafkaConsumerService {
//
//    @KafkaListener(topics = {"atm-topic","personal-data-topic"}, groupId = "consumer")
//    public void listen(String message) {
//        System.out.println("Send message: " + message);
//    }
//
//}
