package com.collab.editor.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    public Room(String roomId2) {
		this.roomId = roomId2;
	}
	@Id
    private String roomId;
    private String name;
    private Set<String> activeUserIds;
    private List<CodeFile> files;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
}
