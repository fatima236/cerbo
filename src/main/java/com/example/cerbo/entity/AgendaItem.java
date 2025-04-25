package com.example.cerbo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "agenda_items")
@Data
public class AgendaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    // Other fields as needed
}