package com.lesonline.tutoringplatform.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nama;
    private String email;
    private String password;
    private String role;
    private String noWhatsapp;
    private String fotoUrl; // Kolom baru untuk URL Foto

    @Column(columnDefinition = "TEXT")
    private String bio;
    private String mataPelajaran;
}