package com.collab.editor.service;

import com.collab.editor.crdt.CRDTDocument;
import com.collab.editor.exception.FileNotFoundException;
import com.collab.editor.exception.RoomNotFoundException;
import com.collab.editor.model.CodeFile;
import com.collab.editor.model.CRDTOperation;
import com.collab.editor.model.Room;
import com.collab.editor.repository.RoomRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CRDTService {
    @Autowired
    private RoomRepository roomRepository;
    
    public void applyOperation(String roomId, String fileId, CRDTOperation operation) {
        Room room = roomRepository.findByRoomId(roomId)
            .orElseThrow(() -> new RoomNotFoundException(roomId));
        
        CodeFile file = room.getFiles().stream()
            .filter(f -> f.getFileId().equals(fileId))
            .findFirst()
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // Initialize CRDT state if null
        if (file.getCrdtState() == null) {
            file.setCrdtState(new CRDTDocument());
        }
        
        // Apply CRDT operation
        file.getCrdtState().applyOperation(operation);
        file.setContent(file.getCrdtState().getText());
        file.setLastModified(LocalDateTime.now());
        
        roomRepository.save(room);
    }
    
    public CRDTOperation createInsertOperation(String userId, int position, String character) {
        return CRDTOperation.builder()
            .operationId(UUID.randomUUID().toString())
            .userId(userId)
            .type(CRDTOperation.OperationType.INSERT)
            .position(position)
            .character(character)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public CRDTOperation createDeleteOperation(String userId, int position) {
        return CRDTOperation.builder()
            .operationId(UUID.randomUUID().toString())
            .userId(userId)
            .type(CRDTOperation.OperationType.DELETE)
            .position(position)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}