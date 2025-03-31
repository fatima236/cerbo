package com.example.cerbo.service.documentService;

import com.example.cerbo.entity.*;
import com.example.cerbo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TrainingRepository trainingRepository;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private ProjectRepository projectRepository;


    @Value("${upload.directory}") // le chemin d'upload
    private String uploadDir;

    @Override
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

    @Override
    public String uploadFile(MultipartFile file,
                             Long eventId,
                             Long articleId,
                             Long projectId,
                             Long trainingId) {

        if (file.isEmpty()) {
            return "Le fichier est vide !";
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();  // Crée le répertoire si il n'existe pas
        }

        // Générer un nom unique pour le fichier
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String filePath = Paths.get(uploadDir, filename).toString();
        System.out.println(filePath);

        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            return "Erreur lors du téléchargement du fichier : " + e.getMessage();
        }

        Document document = new Document();
        document.setName(filename);
        document.setPath(filePath);
        document.setContentType(file.getContentType());
        document.setSize(file.getSize());
        document.setCreationDate(LocalDateTime.now());
        document.setModificationDate(LocalDateTime.now());

        if(eventId != null) {
            document.setEvent(eventRepository.getById(eventId));
        }
        else if(articleId != null) {
            document.setArticle(articleRepository.getById(articleId));
        }
        else if(projectId != null) {
            document.setProject(projectRepository.getById(projectId));
        }
        else if(trainingId != null) {
            document.setTraining(trainingRepository.getById(trainingId));
        }




        try {
            documentRepository.save(document);
        } catch (Exception e) {
            return "Erreur lors de l'enregistrement du fichier dans la base de données : " + e.getMessage();
        }

        return "Fichier uploadé avec succès : " + filePath;
    }

    public List<Document> documentsOfTraining(Long trainingId){
        Training training = trainingRepository.findById(trainingId).orElse(null);
        if(training == null) {return null;}

        return documentRepository.findDocumentsByTrainingId(trainingId);
    }

    public List<Document> documentsOfArticle(Long articleId){
        Article article = articleRepository.findById(articleId).orElse(null);
        if(article == null) {return null;}

        return documentRepository.findDocumentsByArticleId(articleId);
    }

    public List<Document> documentsOfProject(Long projectId){
        Project project = projectRepository.findById(projectId).orElse(null);
        if(project == null) {return null;}

        return documentRepository.findDocumentsByProjectId(projectId);
    }

    public List<Document> documentsOfEvent(Long eventId){
        Event event = eventRepository.findById(eventId).orElse(null);
        if(event == null) {return null;}

        return documentRepository.findDocumentsByEventId(eventId);
    }

    public String removeDocumentById(Long id){
        Document document = documentRepository.findById(id).orElse(null);
        if(document != null) {
            File file = new File(document.getPath());
            if (file.exists() && file.isFile()) {
                boolean fileDeleted = file.delete();
                if (!fileDeleted) {
                    return "Erreur lors de la suppression du fichier : " + document.getPath();
                }
            }
            documentRepository.delete(document);
        }
        else{
            return "Document non trouvé avec l'ID : " + id;
        }
        return "Document supprimé";

    }



}
