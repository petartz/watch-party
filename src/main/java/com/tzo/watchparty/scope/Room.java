package com.tzo.watchparty.scope;

import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private final String roomId;
    private WebSocketSession host;
    private final Map<String, WebSocketSession> viewers = new ConcurrentHashMap<>();

    public Room(String roomId) {
        this.roomId = roomId;
    }

    public WebSocketSession getHost() {
        return host;
    }

    public void setHost(WebSocketSession host) {
        this.host = host;
    }

    public void addViewer(WebSocketSession session) {
        viewers.put(session.getId(), session);
    }

    public void removeViewer(WebSocketSession session) {
        viewers.remove(session.getId());
    }

    public WebSocketSession getParticipant(String id) {
        if (host != null && host.getId().equals(id)) return host;
        return viewers.get(id);
    }

    public void closeAll() {
        try {
            if (host != null) host.close();
            for (WebSocketSession s : viewers.values()) s.close();
        } catch (IOException ignored) {}
    }
}
