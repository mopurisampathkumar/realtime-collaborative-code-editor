package com.collab.editor.model;

import com.collab.editor.crdt.CRDTDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "code_files")
public class CodeFile {
    @Id
    private String id;  // MongoDB document ID
    private String fileId;  // Application-level file ID
    private String fileName;
    private String language;
    private String content;
    private CRDTDocument crdtState;
    private LocalDateTime lastModified;
}