package com.lesonline.tutoringplatform.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idMurid;
    private String namaMurid;
    private Long idGuru;
    private String namaGuru;
    private String mataPelajaran;
    private String noWhatsappGuru;

    // FITUR STATUS & RATING
    private String status = "MENUNGGU"; // MENUNGGU, DISETUJUI, DITOLAK, SELESAI
    private String linkZoom;
    private String catatanGuru;
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String ulasan;

    // FITUR JADWAL LES (TAMBAHKAN INI)
    private String hariLes; // Contoh: "Senin", "Selasa"

    @Column(name = "jam_les")
    private LocalTime jamLes; // Menggunakan LocalTime untuk format jam (HH:mm)

    private LocalDateTime waktuPesan = LocalDateTime.now();
}