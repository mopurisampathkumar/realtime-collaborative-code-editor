package com.collab.editor.exception;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String fileId) {
        super("File not found with ID: " + fileId);
    }
}