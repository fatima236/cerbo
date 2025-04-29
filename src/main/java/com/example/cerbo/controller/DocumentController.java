package com.example.cerbo.controller;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.Project;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.EventRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.service.documentService.DocumentService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private  DocumentRepository documentRepository;
    @Autowired
    private  EventRepository eventRepository;
    @Autowired
    private DocumentService documentService;

    @Value("${upload.directory}")
    private String uploadDir;


    @PostMapping("/upload")
    public ResponseEntity<String>  uploadFile(
                             @RequestParam(value = "file",required = false) MultipartFile file,
                             @RequestParam(value = "eventId",required = false) Long eventId,
                             @RequestParam(value = "articleId",required = false) Long articleId,
                             @RequestParam(value = "projectId",required = false) Long projectId,
                             @RequestParam(value = "trainingId",required = false) Long trainingId
                             ) throws IOException {
            String result = documentService.uploadFile(file, projectId, eventId, articleId, trainingId);
            return ResponseEntity.ok(result);
        }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteDocumentById(@RequestParam(value = "id") Long id){

        String result = documentService.removeDocumentById(id);
        return ResponseEntity.ok(result);    }
}
