package com.collab.editor.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collab.editor.model.ExecutionResult;
import com.collab.editor.service.CodeExecutionService;
import com.collab.editor.websocket.MessageHandler.ExecutionRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExecutionController {
    
	@Autowired
    private CodeExecutionService executionService;
    
    @PostMapping("/execute")
    public ResponseEntity<ExecutionResult> executeCode(@RequestBody ExecutionRequest request) throws IOException {
        ExecutionResult result = executionService.executeCode(
            request.getCode(), 
            request.getLanguage()
        );
        return ResponseEntity.ok(result);
    }
}