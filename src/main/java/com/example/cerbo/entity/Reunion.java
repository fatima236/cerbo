package com.example.cerbo.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "reunions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reunion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date;

    @ManyToMany
    @JoinTable(
            name = "reunion_membres",
            joinColumns = @JoinColumn(name = "reunion_id"),
            inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    private Set<User> membres = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "reunion_projets",
            joinColumns = @JoinColumn(name = "reunion_id"),
            inverseJoinColumns = @JoinColumn(name = "projet_id")
    )
    private List<Project> projetsDiscutes = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String pv;
}