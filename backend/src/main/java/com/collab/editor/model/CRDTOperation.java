package com.collab.editor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CRDTOperation {
	
    private String operationId;
    private String userId;
    private OperationType type;
    private int position;
    private String character;
    private long timestamp;
    private int[] vectorClock;
    
    public enum OperationType {
        INSERT, DELETE
    }
}