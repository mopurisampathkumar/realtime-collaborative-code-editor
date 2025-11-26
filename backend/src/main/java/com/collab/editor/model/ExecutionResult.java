package com.collab.editor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionResult {
	
    private boolean success;
    private String output;
    private String error;
    private long executionTime;
    
    public static ExecutionResult success(String output) {
        return new ExecutionResult(true, output, null, 0);
    }
    
    public static ExecutionResult error(String error) {
        return new ExecutionResult(false, null, error, 0);
    }
}