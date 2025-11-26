package com.collab.editor.crdt;

import com.collab.editor.model.CRDTOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CRDTDocument {
    private List<CRDTChar> characters = new ArrayList<>();
    private Map<String, Integer> vectorClock = new HashMap<>();
    
    public void applyOperation(CRDTOperation op) {
        // Update vector clock
        vectorClock.put(op.getUserId(), 
            vectorClock.getOrDefault(op.getUserId(), 0) + 1);
        
        // Apply operation based on type
        if (op.getType() == CRDTOperation.OperationType.INSERT) {
            insert(op);
        } else if (op.getType() == CRDTOperation.OperationType.DELETE) {
            delete(op);
        }
    }
    
    private void insert(CRDTOperation op) {
        CRDTChar newChar = new CRDTChar(
            op.getCharacter(),
            op.getUserId(),
            op.getTimestamp(),
            op.getPosition(),
            op.getOperationId()
        );
        
        // Insert at position, ensuring bounds
        int insertPos = Math.min(op.getPosition(), characters.size());
        characters.add(insertPos, newChar);
        
        // Update positions of subsequent characters
        for (int i = insertPos + 1; i < characters.size(); i++) {
            characters.get(i).setPosition(i);
        }
    }
    
    private void delete(CRDTOperation op) {
        // Remove character at position
        if (op.getPosition() >= 0 && op.getPosition() < characters.size()) {
            characters.remove(op.getPosition());
            
            // Update positions of subsequent characters
            for (int i = op.getPosition(); i < characters.size(); i++) {
                characters.get(i).setPosition(i);
            }
        }
    }
    
    public String getText() {
        return characters.stream()
            .map(CRDTChar::getCharacter)
            .collect(Collectors.joining());
    }
}

