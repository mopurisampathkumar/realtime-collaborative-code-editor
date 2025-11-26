package com.collab.editor.websocket;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageHandler {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public <T> T parseMessage(String payload, Class<T> clazz) {
        try {
            return objectMapper.readValue(payload, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message: " + e.getMessage());
        }
    }
    
    public String serializeMessage(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message: " + e.getMessage());
        }
    }
    
    @Data
    public static class CodeChangeMessage {
        private String type;
        private String roomId;
        private String fileId;
        private String content;
        private String userId;
        private CRDTOperationDTO operation;
    }
    
    @Data
    public static class UserJoinedMessage {
        private String type = "USER_JOINED";
        private String userId;
        private String username;
        
        public UserJoinedMessage(String username) {
            this.username = username;
            this.userId = username;
        }
    }
    
    @Data
    public static class UserLeftMessage {
        private String type = "USER_LEFT";
        private String userId;
        private String username;
        
        public UserLeftMessage(String username) {
            this.username = username;
            this.userId = username;
        }
    }
    
    @Data
    public static class CRDTOperationDTO {
        private String operationId;
        private String userId;
        private String type;
        private int position;
        private String character;
        private long timestamp;
    }
    
    @Data
    public static class ExecutionRequest {
        private String code;
        private String language;
        private String roomId;
    }
    
    @Data
    public static class CreateRoomRequest {
        private String name;
    }
}