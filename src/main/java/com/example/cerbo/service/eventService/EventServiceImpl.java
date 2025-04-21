package com.example.cerbo.service.eventService;

import com.example.cerbo.entity.Document;
import com.example.cerbo.entity.Event;
import com.example.cerbo.repository.DocumentRepository;
import com.example.cerbo.repository.EventRepository;
import com.example.cerbo.service.documentService.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private DocumentService documentService;

    @Override
    public void deleteEventById(Long id) {
        Event event = eventRepository.getById(id);
        List<Document> documents = documentRepository.findDocumentsByEventId(id);
        for(Document document: documents) {
            documentService.removeDocumentById(document.getId());
        }
        eventRepository.delete(event);
    }

    @Override
    public Event getEventById(Long id) {
        return eventRepository.getById(id);
    }
}
