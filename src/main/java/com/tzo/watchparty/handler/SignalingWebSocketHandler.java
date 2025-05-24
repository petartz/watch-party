package com.tzo.watchparty.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzo.watchparty.config.ObjectMapperConfig;
import com.tzo.watchparty.scope.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SignalingWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    @Qualifier("defaultObjectMapper")
    ObjectMapper objectMapper;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionRoomMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomId(session);
        Room room = rooms.computeIfAbsent(roomId, Room::new);

        if (room.getHost() == null) {
            room.setHost(session);
        } else {
            room.addViewer(session);
            room.getHost().sendMessage(text("{\"type\":\"viewer-joined\", \"senderId\":\"" + session.getId() + "\"}"));
        }
        sessionRoomMap.put(session, roomId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionRoomMap.get(session);
        Room room = rooms.get(roomId);
        if (room != null) {
            if (session.equals(room.getHost())) {
                room.closeAll();
                rooms.remove(roomId);
            } else {
                room.removeViewer(session);
            }
        }
        sessionRoomMap.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = objectMapper.readTree(message.getPayload());
        String type = node.get("type").asText();
        JsonNode payload = node.get("payload");
        String targetId = node.has("targetId") ? node.get("targetId").asText() : null;
        String roomId = sessionRoomMap.get(session);
        Room room = rooms.get(roomId);

        if (room == null) return;

        WebSocketSession target = room.getParticipant(targetId);
        if (target != null) {
            target.sendMessage(text(objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "payload", payload,
                    "senderId", session.getId()
            ))));
        }
    }

    private TextMessage text(String content) {
        return new TextMessage(content);
    }

    private String getRoomId(WebSocketSession session) {
        String path = Objects.requireNonNull(session.getUri()).getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }
}