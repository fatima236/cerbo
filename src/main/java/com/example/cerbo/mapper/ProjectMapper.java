package com.example.cerbo.mapper;

import com.example.cerbo.dto.DocumentDTO;
import com.example.cerbo.dto.ProjectDTO;
import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.Set;
import com.example.cerbo.entity.User;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface ProjectMapper {
    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);

    @Mapping(target = "principalInvestigatorId",
            expression = "java(project.getPrincipalInvestigator() != null ? project.getPrincipalInvestigator().getId() : null)")
    @Mapping(target = "reviewerIds",
            expression = "java(mapReviewersToIds(project.getReviewers()))")
    ProjectDTO toDto(Project project);

    default List<Long> mapReviewersToIds(Set<User> reviewers) {
        if (reviewers == null) {
            return Collections.emptyList();
        }
        return reviewers.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    DocumentDTO toDocumentDto(Document document);
}