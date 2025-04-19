package com.example.cerbo.service.eventService;

import com.example.cerbo.entity.Event;

public interface EventService {
    void deleteEventById(Long id);
    Event getEventById(Long id);
}
