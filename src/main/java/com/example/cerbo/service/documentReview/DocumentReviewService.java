package com.example.cerbo.service.documentReview;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.UserRepository;
import com.example.cerbo.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DocumentReviewService {
    private final DocumentRepository documentRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public DocumentReview createReview(Long documentId, Long reviewerId, String content) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        DocumentReview review = new DocumentReview();
        review.setDocument(document);
        review.setReviewer(reviewer);
        review.setContent(content);
        review.setCreationDate(LocalDateTime.now());
        review.setStatus(RemarkStatus.PENDING);

        return documentReviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<DocumentReview> getReviewsByDocumentId(Long documentId) {
        return documentReviewRepository.findByDocumentId(documentId);
    }

    @Transactional(readOnly = true)
    public List<DocumentReview> getReviewsByProjectId(Long projectId) {
        return documentReviewRepository.findByDocumentProjectId(projectId);
    }

    @Transactional
    public DocumentReview validateReview(Long reviewId, Long adminId, RemarkStatus status, String comment) {
        DocumentReview review = documentReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        review.setStatus(status);
        review.setValidatedBy(admin);
        review.setAdminValidationDate(LocalDateTime.now());
        review.setAdminComment(comment);

        DocumentReview savedReview = documentReviewRepository.save(review);

//        // Notifier l'investigateur si la remarque est validée
//        if (status == RemarkStatus.VALIDATED) {
//            User investigator = review.getDocument().getProject().getPrincipalInvestigator();
//            notificationService.notifyUser(
//                    investigator.getId(),
//                    "Nouvelle remarque validée",
//                    "Une remarque concernant votre projet a été validée."
//            );
//        }

        return savedReview;
    }

    @Transactional
    public DocumentReview respondToReview(Long reviewId, String response, String filePath) {
        DocumentReview review = documentReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setResponse(response);
        review.setResponseDate(LocalDateTime.now());
        review.setResponseFilePath(filePath);

        return documentReviewRepository.save(review);
    }

    @Transactional
    public void finalizeReviews(Long projectId, Long reviewerId) {
        List<DocumentReview> reviews = documentReviewRepository.findByDocumentProjectIdAndReviewer(
                projectId,
                userRepository.getById(reviewerId)
        );

        reviews.forEach(review -> {
            review.setFinalized(true);
        });

        documentReviewRepository.saveAll(reviews);
    }







}
