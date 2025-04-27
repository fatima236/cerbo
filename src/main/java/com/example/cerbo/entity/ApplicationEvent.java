package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.EventType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

    @Getter
    public class ApplicationEvent {
        private final EventType type;
        private final LocalDateTime timestamp = LocalDateTime.now();
        private final Map<String, Object> data;

        public ApplicationEvent(EventType type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }
    }

