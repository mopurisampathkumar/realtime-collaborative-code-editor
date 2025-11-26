package com.collab.editor.service;

import com.collab.editor.exception.RoomNotFoundException;
import com.collab.editor.model.Room;
import com.collab.editor.model.CodeFile;
import com.collab.editor.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    // ✅ Create room normally (REST call)
    public Room createRoom(String name) {
        Room room = new Room();
        room.setRoomId(UUID.randomUUID().toString());
        room.setName(name);
        room.setActiveUserIds(new HashSet<>());
        room.setFiles(new ArrayList<>());
        room.setCreatedAt(LocalDateTime.now());
        room.setLastModified(LocalDateTime.now());

        return roomRepository.save(room);
    }

    // ✅ Auto-create if missing (used by WebSocket flow)
    public Room getOrCreateRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseGet(() -> {
                    Room newRoom = new Room();
                    newRoom.setRoomId(roomId);
                    newRoom.setName("Auto-Created Room");
                    newRoom.setActiveUserIds(new HashSet<>());
                    newRoom.setFiles(new ArrayList<>());
                    newRoom.setCreatedAt(LocalDateTime.now());
                    newRoom.setLastModified(LocalDateTime.now());
                    return roomRepository.save(newRoom);
                });
    }

    // ✅ Strict fetch (use ONLY when you want 404)
    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with ID: " + roomId));
    }

    // ✅ Join room safely (auto-create)
    public Room addUserToRoom(String roomId, String username) {
        Room room = getOrCreateRoom(roomId);
        room.getActiveUserIds().add(username);
        room.setLastModified(LocalDateTime.now());
        return roomRepository.save(room);
    }

    // ✅ Safe removal — does NOT throw if room doesn't exist
    public void removeUserFromRoom(String roomId, String userId) {
        roomRepository.findById(roomId).ifPresent(room -> {
            room.getActiveUserIds().remove(userId);
            room.setLastModified(LocalDateTime.now());
            roomRepository.save(room);
        });
    }

    public Set<String> getActiveUsers(String roomId) {
        return getRoom(roomId).getActiveUserIds();
    }

    public void addFileToRoom(String roomId, CodeFile file) {
        Room room = getRoom(roomId);
        room.getFiles().add(file);
        room.setLastModified(LocalDateTime.now());
        roomRepository.save(room);
    }

    public void updateFile(String roomId, String fileId, String content) {
        Room room = getRoom(roomId);

        room.getFiles().stream()
                .filter(f -> f.getFileId().equals(fileId))
                .findFirst()
                .ifPresent(file -> {
                    file.setContent(content);
                    file.setLastModified(LocalDateTime.now());
                });

        room.setLastModified(LocalDateTime.now());
        roomRepository.save(room);
    }
}
