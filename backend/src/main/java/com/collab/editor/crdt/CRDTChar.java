package com.collab.editor.crdt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class CRDTChar {
    private String character;
    private String userId;
    private long timestamp;
    private int position;
    private String operationId;
}