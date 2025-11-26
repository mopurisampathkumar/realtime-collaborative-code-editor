package com.collab.editor.crdt;

import com.collab.editor.model.CRDTOperation;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CRDTSynchronizer {
    
    private final Map<String, List<CRDTOperation>> operationHistory = new ConcurrentHashMap<>();
    
    public synchronized void addOperation(String documentId, CRDTOperation operation) {
        operationHistory.computeIfAbsent(documentId, k -> new ArrayList<>())
                       .add(operation);
    }
    
    public List<CRDTOperation> getOperations(String documentId, long afterTimestamp) {
        List<CRDTOperation> allOps = operationHistory.getOrDefault(documentId, new ArrayList<>());
        return allOps.stream()
                     .filter(op -> op.getTimestamp() > afterTimestamp)
                     .sorted(Comparator.comparingLong(CRDTOperation::getTimestamp))
                     .toList();
    }
    
    public void clearHistory(String documentId) {
        operationHistory.remove(documentId);
    }
    
    public CRDTOperation resolveConflict(CRDTOperation op1, CRDTOperation op2) {
        // Last-write-wins strategy based on timestamp
        if (op1.getTimestamp() > op2.getTimestamp()) {
            return op1;
        } else if (op2.getTimestamp() > op1.getTimestamp()) {
            return op2;
        } else {
            // If timestamps are equal, use userId for deterministic ordering
            return op1.getUserId().compareTo(op2.getUserId()) > 0 ? op1 : op2;
        }
    }
}