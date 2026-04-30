package com.lesonline.tutoringplatform.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Model User untuk platform LES-IN.
 * Mengelola informasi profil, peran (GURU/MURID), dan jadwal mengajar.
 */
@Entity
@Data // Lombok: Otomatis membuat Getter, Setter, toString, equals, dan hashCode
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- KREDENSIAL AKUN ---
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // "GURU" atau "MURID"

    // --- PROFIL PENGGUNA ---
    @Column(nullable = false)
    private String nama;

    private String noWhatsapp;

    private String fotoUrl; // Menyimpan path atau URL foto profil

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String mataPelajaran; // Spesialisasi pengajaran guru

    // --- FITUR PENJADWALAN TETAP GURU ---
    /**
     * Menyimpan ringkasan jadwal dalam format String.
     * Contoh: "Senin, Rabu, Jumat (08:00 - 12:00)"
     */
    @Column(columnDefinition = "TEXT")
    private String jadwalTetap;

}