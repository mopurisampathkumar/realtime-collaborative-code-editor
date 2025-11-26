package com.collab.editor.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomId) {
        super("Room not found with ID: " + roomId);
    }
}