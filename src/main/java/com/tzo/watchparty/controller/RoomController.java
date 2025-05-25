package com.tzo.watchparty.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    @PostMapping
    public Map<String, String> createRoom() {
        return Map.of("roomId", UUID.randomUUID().toString());
    }
}