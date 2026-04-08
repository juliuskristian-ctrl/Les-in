package com.lesonline.tutoringplatform.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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

    private LocalDateTime waktuPesan = LocalDateTime.now();
    private String status = "MENUNGGU";
}