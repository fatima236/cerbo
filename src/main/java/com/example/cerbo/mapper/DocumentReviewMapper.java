package com.example.cerbo.mapper;

import com.example.cerbo.dto.DocumentReviewDTO;
import com.example.cerbo.entity.DocumentReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

//@Mapper(componentModel = "spring")
//public interface DocumentReviewMapper {
//    DocumentReviewDTO toDto(DocumentReview review);
//
//    @Mapping(target = "reviewerName", expression = "java(review.getReviewer().getFirstName() + ' ' + review.getReviewer().getLastName())")
//    @Mapping(target = "documentName", expression = "java(review.getDocument().getName())")
//    @Mapping(target = "documentType", expression = "java(review.getDocument().getType())")
//    @Mapping(target = "validatedByName", expression = "java(review.getValidatedBy() != null ? review.getValidatedBy().getFirstName() + ' ' + review.getValidatedBy().getLastName() : null)")
//    @Mapping(target = "hasResponseFile", expression = "java(review.getResponseFilePath() != null && !review.getResponseFilePath().isEmpty())")
//    DocumentReviewDTO toDtoWithDetails(DocumentReview review);
//
//    List<DocumentReviewDTO> toDtoList(List<DocumentReview> reviews);
//}