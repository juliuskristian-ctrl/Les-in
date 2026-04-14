package com.lesonline.tutoringplatform.repository;

import com.lesonline.tutoringplatform.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Mencari pesanan berdasarkan ID Guru
    List<Booking> findByIdGuru(Long idGuru);

    // Mencari pesanan berdasarkan ID Murid (Riwayat Murid)
    List<Booking> findByIdMurid(Long idMurid);

    // Untuk validasi duplikat agar tidak pesan guru yang sama berkali-kali
    Optional<Booking> findByIdMuridAndIdGuruAndStatusIn(Long idMurid, Long idGuru, List<String> statuses);
}