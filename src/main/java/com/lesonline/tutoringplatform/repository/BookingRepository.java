package com.lesonline.tutoringplatform.repository;

import com.lesonline.tutoringplatform.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByIdGuru(Long idGuru);
    List<Booking> findByIdMurid(Long idMurid);

    // Untuk mengecek apakah murid sudah memesan guru yang sama sebelumnya
    Optional<Booking> findByIdMuridAndIdGuruAndStatusIn(Long idMurid, Long idGuru, List<String> statuses);
}