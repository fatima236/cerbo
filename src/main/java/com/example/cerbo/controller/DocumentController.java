package com.example.cerbo.controller;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.enums.ReportStatus;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.EventRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.service.FileStorageService;
import com.example.cerbo.service.documentService.DocumentService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
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

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DocumentReviewRepository documentReviewRepository;

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

    @PutMapping("/{docId}/replace")
    public ResponseEntity<?> replaceDoc(@PathVariable("docId") Long docId,
                                               @RequestBody MultipartFile file,
                                               Authentication authentication) throws IOException {
        Optional<Document> optionalDocument  = documentRepository.findById(docId);
        if (optionalDocument .isEmpty()) {
            ResponseEntity.badRequest().body("Le document n'existe pas");
        }
        Document document = optionalDocument.get();
        if(file.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = (User) authentication.getPrincipal();
        String username = user.getUsername();

        if(!document.getProject().getPrincipalInvestigator().getEmail().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tu n'as pas accès à ce document");
        }

        Boolean notexist = !documentReviewRepository.existsByDocument_IdAndReport_Status(docId, ReportStatus.SENT);


        if(notexist){
            return ResponseEntity.badRequest().body("Ce document ne peut pas être modifié");
        }


        String filename = fileStorageService.updateFile(file,document);
        document.setModificationDate(LocalDateTime.now());
        document.setName(filename);

        return  ResponseEntity.ok(documentRepository.save(document));
    }
}
