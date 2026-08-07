package com.example.myapp.controller;

import com.example.myapp.models.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final Logger logger =
            LoggerFactory.getLogger(MessageController.class);

    private final List<Message> messages = new ArrayList<>();
    private Long counter = 1L;

    @GetMapping
    public List<Message> getMessages() {
        logger.info("GET /api/messages");
        return messages;
    }

    @PostMapping
    public Message createMessage(@RequestBody Message message) {
        logger.info("POST /api/messages : {}", message.getText());

        message.setId(counter++);
        messages.add(message);

        logger.info("Message saved with id={}", message.getId());

        return message;
    }

    @GetMapping("/error")
    public String error() {
        throw new RuntimeException("Demo error for Datadog");
    }
}
