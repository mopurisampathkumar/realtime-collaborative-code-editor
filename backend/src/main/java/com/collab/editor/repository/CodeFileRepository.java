package com.collab.editor.repository;

import com.collab.editor.model.CodeFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeFileRepository extends MongoRepository<CodeFile, String> {
    Optional<CodeFile> findByFileId(String fileId);
    List<CodeFile> findByFileNameContaining(String fileName);
}