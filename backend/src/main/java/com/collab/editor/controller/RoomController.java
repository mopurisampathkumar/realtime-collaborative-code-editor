package com.collab.editor.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collab.editor.model.Room;
import com.collab.editor.service.RoomService;
import com.collab.editor.websocket.MessageHandler.CreateRoomRequest;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {
    
	@Autowired
    private RoomService roomService;
    
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request.getName());
        return ResponseEntity.ok(room);
    }
    
    @GetMapping("/{roomId}")
    public ResponseEntity<Room> getRoom(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        return ResponseEntity.ok(room);
    }
    
    @GetMapping("/{roomId}/users")
    public ResponseEntity<Set<String>> getActiveUsers(@PathVariable String roomId) {
        Set<String> users = roomService.getActiveUsers(roomId);
        return ResponseEntity.ok(users);
    }
}