package com.collab.editor.websocket;

import com.collab.editor.crdt.CRDTSynchronizer;
import com.collab.editor.model.CRDTOperation;
import com.collab.editor.service.CRDTService;
import com.collab.editor.service.RoomService;
import com.collab.editor.websocket.MessageHandler.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    
    private final CRDTService crdtService;
    private final RoomService roomService;
    private final MessageHandler messageHandler;
    private final CRDTSynchronizer crdtSynchronizer;
    
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = extractParam(session, "roomId");
        String username = extractParam(session, "username");
        
        if (roomId == null || username == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        
        sessionToRoom.put(session.getId(), roomId);
        sessionToUser.put(session.getId(), username);
        
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        roomService.addUserToRoom(roomId, username);
        
        // Send current users list to new user
        Set<String> activeUsers = roomService.getActiveUsers(roomId);
        Map<String, Object> usersList = new HashMap<>();
        usersList.put("type", "USERS_LIST");
        usersList.put("users", activeUsers.stream()
            .map(user -> {
                Map<String, String> userMap = new HashMap<>();
                userMap.put("id", user);
                userMap.put("name", user);
                return userMap;
            })
            .toList());
        sendMessage(session, messageHandler.serializeMessage(usersList));
        
        // Broadcast user joined to others
        broadcastToRoom(roomId, new UserJoinedMessage(username), session);
        
        System.out.println("User " + username + " joined room " + roomId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message: " + payload);
        
        CodeChangeMessage msg = messageHandler.parseMessage(payload, CodeChangeMessage.class);
        String roomId = sessionToRoom.get(session.getId());
        String username = sessionToUser.get(session.getId());
        
        if (roomId == null) {
            return;
        }
        
        // Handle different message types
        switch (msg.getType()) {
            case "CODE_CHANGE":
                handleCodeChange(roomId, msg, session, username);
                break;
            case "FILE_CREATE":
                handleFileCreate(roomId, msg, session);
                break;
            case "FILE_SAVE":
                handleFileSave(roomId, msg, session);
                break;
            case "CURSOR_MOVE":
                broadcastToRoom(roomId, msg, session);
                break;
            default:
                System.out.println("Unknown message type: " + msg.getType());
        }
    }
    
    private void handleCodeChange(String roomId, CodeChangeMessage msg, WebSocketSession sender, String username) {
        System.out.println("Code change in room " + roomId + " by " + username);
        
        // Apply CRDT operation if provided
        if (msg.getOperation() != null) {
            CRDTOperation operation = convertToOperation(msg.getOperation());
            crdtService.applyOperation(roomId, msg.getFileId(), operation);
            crdtSynchronizer.addOperation(roomId + ":" + msg.getFileId(), operation);
        }
        
        // Update file content directly
        if (msg.getContent() != null && msg.getFileId() != null) {
            try {
                roomService.updateFile(roomId, msg.getFileId(), msg.getContent());
            } catch (Exception e) {
                System.err.println("Error updating file: " + e.getMessage());
            }
        }
        
        // Prepare broadcast message
        Map<String, Object> broadcastMsg = new HashMap<>();
        broadcastMsg.put("type", "CODE_UPDATE");
        broadcastMsg.put("content", msg.getContent());
        broadcastMsg.put("fileId", msg.getFileId());
        broadcastMsg.put("userId", username);
        broadcastMsg.put("roomId", roomId);
        
        // Broadcast to all users in room EXCEPT sender
        broadcastToRoom(roomId, broadcastMsg, sender);
        
        System.out.println("Broadcasted code change to room " + roomId);
    }
    
    private void handleFileCreate(String roomId, CodeChangeMessage msg, WebSocketSession sender) {
        System.out.println("File creation in room " + roomId);
        broadcastToRoom(roomId, msg, sender);
    }
    
    private void handleFileSave(String roomId, CodeChangeMessage msg, WebSocketSession sender) {
        System.out.println("File save in room " + roomId);
        if (msg.getFileId() != null && msg.getContent() != null) {
            roomService.updateFile(roomId, msg.getFileId(), msg.getContent());
        }
        broadcastToRoom(roomId, msg, sender);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.remove(session.getId());
        String username = sessionToUser.remove(session.getId());
        
        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomId);
                }
            }
            
            if (username != null) {
                roomService.removeUserFromRoom(roomId, username);
                broadcastToRoom(roomId, new UserLeftMessage(username), null);
                System.out.println("User " + username + " left room " + roomId);
            }
        }
    }
    
    private void broadcastToRoom(String roomId, Object message, WebSocketSession exclude) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            String json = messageHandler.serializeMessage(message);
            int sentCount = 0;
            for (WebSocketSession s : sessions) {
                if (!s.equals(exclude)) {
                    sendMessage(s, json);
                    sentCount++;
                }
            }
            System.out.println("Message sent to " + sentCount + " users in room " + roomId);
        }
    }
    
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String extractParam(WebSocketSession session, String param) {
        URI uri = session.getUri();
        if (uri != null) {
            return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst(param);
        }
        return null;
    }
    
    private CRDTOperation convertToOperation(CRDTOperationDTO dto) {
        CRDTOperation op = new CRDTOperation();
        op.setOperationId(dto.getOperationId());
        op.setUserId(dto.getUserId());
        op.setType(CRDTOperation.OperationType.valueOf(dto.getType()));
        op.setPosition(dto.getPosition());
        op.setCharacter(dto.getCharacter());
        op.setTimestamp(dto.getTimestamp());
        return op;
    }
}